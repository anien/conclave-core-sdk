#include <stdio.h>
#include <assert.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <string>
#include <stdexcept>
#include "common.hpp"

#include "persistent_disk.hpp"

#define FATFS_PRINT_READ_WRITE 0

#if FATFS_PRINT_READ_WRITE
#define FATFS_DEBUG_PRINT_RW(parms...) FATFS_DEBUG_PRINT(parms);
#else
#define FATFS_DEBUG_PRINT_RW(...)
#endif

static jobject obj;
static JavaVM *jvm = NULL;

/*
  This is called by Java/Kotlin (FileSystemHandler.kt) during the setup of the files
    that represent the FatFs persistent-encrypted filesystems.
    It is only needed to set the global reference of the JavaVM
*/
JNIEXPORT void JNICALL Java_com_r3_conclave_host_internal_fatfs_FileSystemHandler_setup(JNIEnv* input_env,
											jobject input_obj) {
    DEBUG_PRINT_FUNCTION;
    jint res = input_env->GetJavaVM(&jvm);

    if (res != JNI_OK) {
	FATFS_DEBUG_PRINT("JNI Crashed %d\n", -1);
	return;
    }
    obj = input_env->NewGlobalRef(input_obj);
    auto cls = input_env->GetObjectClass(obj);

    if (cls == nullptr) {
	FATFS_DEBUG_PRINT("Class not found %d\n", -1);
    }    
}


/*
  This is called by Java/Kotlin (FileSystemHandler.kt) at the closing of the class instance
    that handles the files representing the FatFs persistent-encrypted filesystems.
  It is only needed to clean up the global reference of the JavaVM 
*/
JNIEXPORT void JNICALL Java_com_r3_conclave_host_internal_fatfs_FileSystemHandler_cleanup(JNIEnv* input_env,
											  jobject) {
    DEBUG_PRINT_FUNCTION;
    input_env->DeleteGlobalRef(obj);
    jvm = NULL;
    obj = NULL;
}

/*
  Call the Java/Kotkin layer to get the size of the file that represents the filesystem,
    to understand if the file is present or needs to be created and then to establish if the
    filesystem needs to be initialized or just loaded.
*/
long host_disk_get_size(const unsigned char drive) {
    DEBUG_PRINT_FUNCTION;
    JNIEnv* env = NULL;
    jint rs = jvm->AttachCurrentThread((void**)&env, NULL);

    if (rs != JNI_OK) {
	FATFS_DEBUG_PRINT("JNI Crash %d\n", drive);
	return -1;
    }
    auto cls = env->GetObjectClass(obj);

    if (cls == nullptr) {
	jvm->DetachCurrentThread();	
	return -1;
    }    
    jmethodID mid = env->GetMethodID(cls, "getDriveSize", "(I)J");
    
    if (mid == nullptr) {
	FATFS_DEBUG_PRINT("Host not getting the file size of drive %d\n", drive);
	jvm->DetachCurrentThread();
	return -1;
    }    	
    const long res = env->CallLongMethod(obj,
					 mid,
					 static_cast<int>(drive));
    jvm->DetachCurrentThread();
    return res;
}

/*
  Call Java/Kotlin (FileSystemHandler.kt) to read bytes from the file that represents the filesystem.
*/
int host_disk_read(const unsigned char drive,
		   const unsigned int sector_id,
		   const unsigned char num_sectors,
		   const unsigned int sector_size,
		   unsigned char* buf) {
    FATFS_DEBUG_PRINT_RW("Read - Sector Id %d - Num %d - Size %d - Drive %d\n", sector_id, num_sectors, sector_size, drive);

    JNIEnv* env = NULL;
    jint rs = jvm->AttachCurrentThread((void**)&env, NULL);

    if (rs != JNI_OK) {
	FATFS_DEBUG_PRINT("JNI Crash %d\n", drive);
	return -1;
    }
    auto cls = env->GetObjectClass(obj);

    if (cls == nullptr) {
	jvm->DetachCurrentThread();	
	FATFS_DEBUG_PRINT("JNI No class found %d\n", drive);
	return -1;
    }
    const jmethodID mid = env->GetMethodID(cls, "read", "(IIII)[B");
    
    if (mid == nullptr) {
	jvm->DetachCurrentThread();
	FATFS_DEBUG_PRINT("JNI No method found %d\n", drive);
	return -1;
    }    
    jbyteArray read_buffer = (jbyteArray) env->CallObjectMethod(obj,
								mid,
								static_cast<int>(drive),
								sector_id,
								static_cast<int>(num_sectors),
								sector_size);
    if (read_buffer == NULL) {
	jvm->DetachCurrentThread();
	FATFS_DEBUG_PRINT("JNI No buffer found %d\n", drive);
	return -1;
    }
    jsize len = env->GetArrayLength(read_buffer);
    jbyte* jbuf = env->GetByteArrayElements(read_buffer, 0);
    memcpy(buf, (unsigned char*)jbuf, len);
    env->ReleaseByteArrayElements(read_buffer, jbuf, 0);
    jvm->DetachCurrentThread();
    return len;
}


/*
  Call Java/Kotlin (FileSystemHandler.kt) to write bytes to the file that represents the filesystem.
*/
int host_disk_write(const unsigned char drive,
		    const unsigned char* buf,
		    const unsigned int num_writes,
		    const unsigned int sector_size,
		    const unsigned int* indices) {
    FATFS_DEBUG_PRINT_RW("Drive %d, Num writes %d, Sector size %d\n", drive, num_writes, sector_size);

    JNIEnv* env = NULL;
    jint rs = jvm->AttachCurrentThread((void**)&env, NULL);

    if (rs != JNI_OK) {
	FATFS_DEBUG_PRINT("JNI Crash %d\n", drive);
	return -1;
    }
    auto cls = env->GetObjectClass(obj);
    
    if (cls == nullptr) {
	FATFS_DEBUG_PRINT("Class not found %d\n", drive);
	jvm->DetachCurrentThread();
	return -1;
    }
    jmethodID mid = env->GetMethodID(cls, "write", "(I[BI[I)I");

    if (mid == nullptr) {
	FATFS_DEBUG_PRINT("Host not writing to drive %d, method not found\n", drive);
	jvm->DetachCurrentThread();
	return -1;
    }
    const unsigned int buffer_size = num_writes * sector_size;
    jbyteArray jbuf_array = env->NewByteArray(buffer_size);

    if (jbuf_array == NULL) {
	FATFS_DEBUG_PRINT("Host not writing to drive %d, byte array not created\n", drive);
	jvm->DetachCurrentThread();
	return -1;
    }	
    jintArray jindices_array = env->NewIntArray(num_writes);

    if (jindices_array == NULL) {
	FATFS_DEBUG_PRINT("Host not writing to drive %d, int array not created\n", drive);
	jvm->DetachCurrentThread();
	return -1;
    }	
    env->SetByteArrayRegion(jbuf_array, 0, buffer_size, (const jbyte*)buf);
    env->SetIntArrayRegion(jindices_array, 0, num_writes, (const jint*)indices);

    const int res = env->CallIntMethod(obj,
				       mid,
				       static_cast<int>(drive),
				       jbuf_array,
				       sector_size,
				       jindices_array);

    jvm->DetachCurrentThread();
    return res;
}
