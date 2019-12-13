// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: attestation.proto

package com.r3.sgx.core.common;

/**
 * Protobuf type {@code EpidEnclaveMessage}
 */
public  final class EpidEnclaveMessage extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:EpidEnclaveMessage)
    EpidEnclaveMessageOrBuilder {
private static final long serialVersionUID = 0L;
  // Use EpidEnclaveMessage.newBuilder() to construct.
  private EpidEnclaveMessage(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private EpidEnclaveMessage() {
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private EpidEnclaveMessage(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            com.r3.sgx.core.common.GetReportReply.Builder subBuilder = null;
            if (epidEnclaveMessageCase_ == 1) {
              subBuilder = ((com.r3.sgx.core.common.GetReportReply) epidEnclaveMessage_).toBuilder();
            }
            epidEnclaveMessage_ =
                input.readMessage(com.r3.sgx.core.common.GetReportReply.PARSER, extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom((com.r3.sgx.core.common.GetReportReply) epidEnclaveMessage_);
              epidEnclaveMessage_ = subBuilder.buildPartial();
            }
            epidEnclaveMessageCase_ = 1;
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.r3.sgx.core.common.Attestation.internal_static_EpidEnclaveMessage_descriptor;
  }

  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.r3.sgx.core.common.Attestation.internal_static_EpidEnclaveMessage_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.r3.sgx.core.common.EpidEnclaveMessage.class, com.r3.sgx.core.common.EpidEnclaveMessage.Builder.class);
  }

  private int bitField0_;
  private int epidEnclaveMessageCase_ = 0;
  private java.lang.Object epidEnclaveMessage_;
  public enum EpidEnclaveMessageCase
      implements com.google.protobuf.Internal.EnumLite {
    GET_REPORT_REPLY(1),
    EPIDENCLAVEMESSAGE_NOT_SET(0);
    private final int value;
    private EpidEnclaveMessageCase(int value) {
      this.value = value;
    }
    /**
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @java.lang.Deprecated
    public static EpidEnclaveMessageCase valueOf(int value) {
      return forNumber(value);
    }

    public static EpidEnclaveMessageCase forNumber(int value) {
      switch (value) {
        case 1: return GET_REPORT_REPLY;
        case 0: return EPIDENCLAVEMESSAGE_NOT_SET;
        default: return null;
      }
    }
    public int getNumber() {
      return this.value;
    }
  };

  public EpidEnclaveMessageCase
  getEpidEnclaveMessageCase() {
    return EpidEnclaveMessageCase.forNumber(
        epidEnclaveMessageCase_);
  }

  public static final int GET_REPORT_REPLY_FIELD_NUMBER = 1;
  /**
   * <code>optional .GetReportReply get_report_reply = 1;</code>
   */
  public boolean hasGetReportReply() {
    return epidEnclaveMessageCase_ == 1;
  }
  /**
   * <code>optional .GetReportReply get_report_reply = 1;</code>
   */
  public com.r3.sgx.core.common.GetReportReply getGetReportReply() {
    if (epidEnclaveMessageCase_ == 1) {
       return (com.r3.sgx.core.common.GetReportReply) epidEnclaveMessage_;
    }
    return com.r3.sgx.core.common.GetReportReply.getDefaultInstance();
  }
  /**
   * <code>optional .GetReportReply get_report_reply = 1;</code>
   */
  public com.r3.sgx.core.common.GetReportReplyOrBuilder getGetReportReplyOrBuilder() {
    if (epidEnclaveMessageCase_ == 1) {
       return (com.r3.sgx.core.common.GetReportReply) epidEnclaveMessage_;
    }
    return com.r3.sgx.core.common.GetReportReply.getDefaultInstance();
  }

  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    if (hasGetReportReply()) {
      if (!getGetReportReply().isInitialized()) {
        memoizedIsInitialized = 0;
        return false;
      }
    }
    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (epidEnclaveMessageCase_ == 1) {
      output.writeMessage(1, (com.r3.sgx.core.common.GetReportReply) epidEnclaveMessage_);
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (epidEnclaveMessageCase_ == 1) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, (com.r3.sgx.core.common.GetReportReply) epidEnclaveMessage_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof com.r3.sgx.core.common.EpidEnclaveMessage)) {
      return super.equals(obj);
    }
    com.r3.sgx.core.common.EpidEnclaveMessage other = (com.r3.sgx.core.common.EpidEnclaveMessage) obj;

    boolean result = true;
    result = result && getEpidEnclaveMessageCase().equals(
        other.getEpidEnclaveMessageCase());
    if (!result) return false;
    switch (epidEnclaveMessageCase_) {
      case 1:
        result = result && getGetReportReply()
            .equals(other.getGetReportReply());
        break;
      case 0:
      default:
    }
    result = result && unknownFields.equals(other.unknownFields);
    return result;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    switch (epidEnclaveMessageCase_) {
      case 1:
        hash = (37 * hash) + GET_REPORT_REPLY_FIELD_NUMBER;
        hash = (53 * hash) + getGetReportReply().hashCode();
        break;
      case 0:
      default:
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.r3.sgx.core.common.EpidEnclaveMessage parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.r3.sgx.core.common.EpidEnclaveMessage parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.r3.sgx.core.common.EpidEnclaveMessage parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.r3.sgx.core.common.EpidEnclaveMessage parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.r3.sgx.core.common.EpidEnclaveMessage parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.r3.sgx.core.common.EpidEnclaveMessage parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.r3.sgx.core.common.EpidEnclaveMessage parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.r3.sgx.core.common.EpidEnclaveMessage parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.r3.sgx.core.common.EpidEnclaveMessage parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static com.r3.sgx.core.common.EpidEnclaveMessage parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.r3.sgx.core.common.EpidEnclaveMessage parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.r3.sgx.core.common.EpidEnclaveMessage parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.r3.sgx.core.common.EpidEnclaveMessage prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code EpidEnclaveMessage}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:EpidEnclaveMessage)
      com.r3.sgx.core.common.EpidEnclaveMessageOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.r3.sgx.core.common.Attestation.internal_static_EpidEnclaveMessage_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.r3.sgx.core.common.Attestation.internal_static_EpidEnclaveMessage_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.r3.sgx.core.common.EpidEnclaveMessage.class, com.r3.sgx.core.common.EpidEnclaveMessage.Builder.class);
    }

    // Construct using com.r3.sgx.core.common.EpidEnclaveMessage.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    public Builder clear() {
      super.clear();
      epidEnclaveMessageCase_ = 0;
      epidEnclaveMessage_ = null;
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.r3.sgx.core.common.Attestation.internal_static_EpidEnclaveMessage_descriptor;
    }

    public com.r3.sgx.core.common.EpidEnclaveMessage getDefaultInstanceForType() {
      return com.r3.sgx.core.common.EpidEnclaveMessage.getDefaultInstance();
    }

    public com.r3.sgx.core.common.EpidEnclaveMessage build() {
      com.r3.sgx.core.common.EpidEnclaveMessage result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public com.r3.sgx.core.common.EpidEnclaveMessage buildPartial() {
      com.r3.sgx.core.common.EpidEnclaveMessage result = new com.r3.sgx.core.common.EpidEnclaveMessage(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      if (epidEnclaveMessageCase_ == 1) {
        if (getReportReplyBuilder_ == null) {
          result.epidEnclaveMessage_ = epidEnclaveMessage_;
        } else {
          result.epidEnclaveMessage_ = getReportReplyBuilder_.build();
        }
      }
      result.bitField0_ = to_bitField0_;
      result.epidEnclaveMessageCase_ = epidEnclaveMessageCase_;
      onBuilt();
      return result;
    }

    public Builder clone() {
      return (Builder) super.clone();
    }
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return (Builder) super.setField(field, value);
    }
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return (Builder) super.clearField(field);
    }
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return (Builder) super.clearOneof(oneof);
    }
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return (Builder) super.setRepeatedField(field, index, value);
    }
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return (Builder) super.addRepeatedField(field, value);
    }
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.r3.sgx.core.common.EpidEnclaveMessage) {
        return mergeFrom((com.r3.sgx.core.common.EpidEnclaveMessage)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.r3.sgx.core.common.EpidEnclaveMessage other) {
      if (other == com.r3.sgx.core.common.EpidEnclaveMessage.getDefaultInstance()) return this;
      switch (other.getEpidEnclaveMessageCase()) {
        case GET_REPORT_REPLY: {
          mergeGetReportReply(other.getGetReportReply());
          break;
        }
        case EPIDENCLAVEMESSAGE_NOT_SET: {
          break;
        }
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    public final boolean isInitialized() {
      if (hasGetReportReply()) {
        if (!getGetReportReply().isInitialized()) {
          return false;
        }
      }
      return true;
    }

    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      com.r3.sgx.core.common.EpidEnclaveMessage parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.r3.sgx.core.common.EpidEnclaveMessage) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int epidEnclaveMessageCase_ = 0;
    private java.lang.Object epidEnclaveMessage_;
    public EpidEnclaveMessageCase
        getEpidEnclaveMessageCase() {
      return EpidEnclaveMessageCase.forNumber(
          epidEnclaveMessageCase_);
    }

    public Builder clearEpidEnclaveMessage() {
      epidEnclaveMessageCase_ = 0;
      epidEnclaveMessage_ = null;
      onChanged();
      return this;
    }

    private int bitField0_;

    private com.google.protobuf.SingleFieldBuilderV3<
        com.r3.sgx.core.common.GetReportReply, com.r3.sgx.core.common.GetReportReply.Builder, com.r3.sgx.core.common.GetReportReplyOrBuilder> getReportReplyBuilder_;
    /**
     * <code>optional .GetReportReply get_report_reply = 1;</code>
     */
    public boolean hasGetReportReply() {
      return epidEnclaveMessageCase_ == 1;
    }
    /**
     * <code>optional .GetReportReply get_report_reply = 1;</code>
     */
    public com.r3.sgx.core.common.GetReportReply getGetReportReply() {
      if (getReportReplyBuilder_ == null) {
        if (epidEnclaveMessageCase_ == 1) {
          return (com.r3.sgx.core.common.GetReportReply) epidEnclaveMessage_;
        }
        return com.r3.sgx.core.common.GetReportReply.getDefaultInstance();
      } else {
        if (epidEnclaveMessageCase_ == 1) {
          return getReportReplyBuilder_.getMessage();
        }
        return com.r3.sgx.core.common.GetReportReply.getDefaultInstance();
      }
    }
    /**
     * <code>optional .GetReportReply get_report_reply = 1;</code>
     */
    public Builder setGetReportReply(com.r3.sgx.core.common.GetReportReply value) {
      if (getReportReplyBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        epidEnclaveMessage_ = value;
        onChanged();
      } else {
        getReportReplyBuilder_.setMessage(value);
      }
      epidEnclaveMessageCase_ = 1;
      return this;
    }
    /**
     * <code>optional .GetReportReply get_report_reply = 1;</code>
     */
    public Builder setGetReportReply(
        com.r3.sgx.core.common.GetReportReply.Builder builderForValue) {
      if (getReportReplyBuilder_ == null) {
        epidEnclaveMessage_ = builderForValue.build();
        onChanged();
      } else {
        getReportReplyBuilder_.setMessage(builderForValue.build());
      }
      epidEnclaveMessageCase_ = 1;
      return this;
    }
    /**
     * <code>optional .GetReportReply get_report_reply = 1;</code>
     */
    public Builder mergeGetReportReply(com.r3.sgx.core.common.GetReportReply value) {
      if (getReportReplyBuilder_ == null) {
        if (epidEnclaveMessageCase_ == 1 &&
            epidEnclaveMessage_ != com.r3.sgx.core.common.GetReportReply.getDefaultInstance()) {
          epidEnclaveMessage_ = com.r3.sgx.core.common.GetReportReply.newBuilder((com.r3.sgx.core.common.GetReportReply) epidEnclaveMessage_)
              .mergeFrom(value).buildPartial();
        } else {
          epidEnclaveMessage_ = value;
        }
        onChanged();
      } else {
        if (epidEnclaveMessageCase_ == 1) {
          getReportReplyBuilder_.mergeFrom(value);
        }
        getReportReplyBuilder_.setMessage(value);
      }
      epidEnclaveMessageCase_ = 1;
      return this;
    }
    /**
     * <code>optional .GetReportReply get_report_reply = 1;</code>
     */
    public Builder clearGetReportReply() {
      if (getReportReplyBuilder_ == null) {
        if (epidEnclaveMessageCase_ == 1) {
          epidEnclaveMessageCase_ = 0;
          epidEnclaveMessage_ = null;
          onChanged();
        }
      } else {
        if (epidEnclaveMessageCase_ == 1) {
          epidEnclaveMessageCase_ = 0;
          epidEnclaveMessage_ = null;
        }
        getReportReplyBuilder_.clear();
      }
      return this;
    }
    /**
     * <code>optional .GetReportReply get_report_reply = 1;</code>
     */
    public com.r3.sgx.core.common.GetReportReply.Builder getGetReportReplyBuilder() {
      return getGetReportReplyFieldBuilder().getBuilder();
    }
    /**
     * <code>optional .GetReportReply get_report_reply = 1;</code>
     */
    public com.r3.sgx.core.common.GetReportReplyOrBuilder getGetReportReplyOrBuilder() {
      if ((epidEnclaveMessageCase_ == 1) && (getReportReplyBuilder_ != null)) {
        return getReportReplyBuilder_.getMessageOrBuilder();
      } else {
        if (epidEnclaveMessageCase_ == 1) {
          return (com.r3.sgx.core.common.GetReportReply) epidEnclaveMessage_;
        }
        return com.r3.sgx.core.common.GetReportReply.getDefaultInstance();
      }
    }
    /**
     * <code>optional .GetReportReply get_report_reply = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        com.r3.sgx.core.common.GetReportReply, com.r3.sgx.core.common.GetReportReply.Builder, com.r3.sgx.core.common.GetReportReplyOrBuilder> 
        getGetReportReplyFieldBuilder() {
      if (getReportReplyBuilder_ == null) {
        if (!(epidEnclaveMessageCase_ == 1)) {
          epidEnclaveMessage_ = com.r3.sgx.core.common.GetReportReply.getDefaultInstance();
        }
        getReportReplyBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            com.r3.sgx.core.common.GetReportReply, com.r3.sgx.core.common.GetReportReply.Builder, com.r3.sgx.core.common.GetReportReplyOrBuilder>(
                (com.r3.sgx.core.common.GetReportReply) epidEnclaveMessage_,
                getParentForChildren(),
                isClean());
        epidEnclaveMessage_ = null;
      }
      epidEnclaveMessageCase_ = 1;
      onChanged();;
      return getReportReplyBuilder_;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:EpidEnclaveMessage)
  }

  // @@protoc_insertion_point(class_scope:EpidEnclaveMessage)
  private static final com.r3.sgx.core.common.EpidEnclaveMessage DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.r3.sgx.core.common.EpidEnclaveMessage();
  }

  public static com.r3.sgx.core.common.EpidEnclaveMessage getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  @java.lang.Deprecated public static final com.google.protobuf.Parser<EpidEnclaveMessage>
      PARSER = new com.google.protobuf.AbstractParser<EpidEnclaveMessage>() {
    public EpidEnclaveMessage parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new EpidEnclaveMessage(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<EpidEnclaveMessage> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<EpidEnclaveMessage> getParserForType() {
    return PARSER;
  }

  public com.r3.sgx.core.common.EpidEnclaveMessage getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

