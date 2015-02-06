package org.texai.skill.fileTransfer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.texai.util.StringUtils;

/**
 * FileTransferInfo.java
 *
 * Description:
 *
 * Copyright (C) Feb 3, 2015, Stephen L. Reed.
 */
public final class FileTransferInfo {

  // the maximum size of the file chunk
  public static final int MAXIMUM_FILE_CHUNK_SIZE = 8192;

  // the file transfer conversation id
  private final UUID conversationId;
  // the sender file path
  private final String senderFilePath;
  // the requester qualified name
  private String requesterQualifiedName;
  // the requester service
  private String requesterService;
  // the recipient file path
  private final String recipientFilePath;
  // the sender container name
  private final String senderContainerName;
  // the recipient container name
  private final String recipientContainerName;
  // the starting date time of the file transfer
  private final DateTime startingDateTime = new DateTime();
  // the ending date time of the file transfer
  private DateTime endingDateTime;
  // the hash of the transferred file's contents
  private String fileHash;
  // the size of the transferred file's contents
  private long fileSize = -1;
  // the file transfer state
  private FileTransferState fileTransferState = FileTransferState.UNINITIALIZED;
  // the sender's file input stream
  private BufferedInputStream bufferedInputStream;
  // the receiver's file output stream
  private BufferedOutputStream bufferedOutputStream;
  // the last sent file chunk bytes
  private byte[] fileChunkBytes;
  // the number of transferred file chunks
  private int fileChunksCnt = 0;

  // the file transfer states
  public enum FileTransferState {

    UNINITIALIZED,
    OK_TO_SEND,
    OK_TO_RECEIVE,
    FILE_TRANSFER_STARTED,
    FILE_TRANSFER_COMPLETE
  }

  //
  // @param fileTransferState the given file transfer state
  // @return a string representation of the given file transfer state
  public static String fileTransferStateToString(final FileTransferState fileTransferState) {
    if (fileTransferState.equals(FileTransferState.UNINITIALIZED)) {
      return "uninitialized";
    } else if (fileTransferState.equals(FileTransferState.OK_TO_SEND)) {
      return "OK to send";
    } else if (fileTransferState.equals(FileTransferState.OK_TO_RECEIVE)) {
      return "OK to receive";
    } else if (fileTransferState.equals(FileTransferState.FILE_TRANSFER_STARTED)) {
      return "file transfer started";
    } else if (fileTransferState.equals(FileTransferState.FILE_TRANSFER_COMPLETE)) {
      return "file transfer complete";
    } else {
      assert false;
      return null;
    }
  }

  /**
   * Creates a new FileTransferRequestInfo instance.
   *
   * @param conversationId the file transfer conversation id
   * @param senderFilePath the sender file path
   * @param recipientFilePath the recipient file path
   * @param senderContainerName the sender container name
   * @param recipientContainerName the starting date time of the file transfer
   */
  FileTransferInfo(
          final UUID conversationId,
          final String senderFilePath,
          final String recipientFilePath,
          final String senderContainerName,
          final String recipientContainerName) {
    //Preconditions
    assert conversationId != null : "conversationId must not be null";
    assert StringUtils.isNonEmptyString(senderFilePath) : "senderFilePath must be a non-empty string";
    assert StringUtils.isNonEmptyString(recipientFilePath) : "recipientFilePath must be a non-empty string";
    assert StringUtils.isNonEmptyString(senderContainerName) : "senderContainerName must be a non-empty string";
    assert StringUtils.isNonEmptyString(recipientContainerName) : "recipientContainerName must be a non-empty string";

    this.conversationId = conversationId;
    this.senderFilePath = senderFilePath;
    this.recipientFilePath = recipientFilePath;
    this.senderContainerName = senderContainerName;
    this.recipientContainerName = recipientContainerName;
  }

  /**
   * Returns a brief string representation of this object.
   *
   * @return a brief string representation of this object
   */
  public String toBriefString() {
    return (new StringBuilder())
            .append('[')
            .append(senderContainerName)
            .append(':')
            .append(senderFilePath)
            .append(" --> ")
            .append(recipientContainerName)
            .append(':')
            .append(recipientFilePath)
            .append(']')
            .toString();
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
            .append('[')
            .append(senderContainerName)
            .append(':')
            .append(senderFilePath)
            .append(" --> ")
            .append(recipientContainerName)
            .append(':')
            .append(recipientFilePath)
            .append("\nstarted: ")
            .append(startingDateTime)
            .append('\n');
    if (endingDateTime != null) {
      stringBuilder
              .append("duration: ")
              .append(Seconds.secondsBetween(startingDateTime, endingDateTime))
              .append(" seconds\n");
    }
    if (fileHash != null) {
      stringBuilder
              .append("file hash: ")
              .append(fileHash)
              .append('\n');
    }
    if (fileHash != null) {
      stringBuilder
              .append("file hash: ")
              .append(fileHash)
              .append('\n');
    }
    if (fileSize > -1) {
      stringBuilder
              .append("file size: ")
              .append(fileSize)
              .append(" bytes\n");
    }
    stringBuilder
            .append("file transfer state: ")
            .append(fileTransferStateToString(fileTransferState))
            .append('\n');
    if (fileChunksCnt > -1) {
      stringBuilder
              .append("file chunks transfered: ")
              .append(fileChunksCnt)
              .append('\n');
    }
    if (fileChunkBytes != null) {
      stringBuilder
              .append("last sent file chunk size: ")
              .append(fileChunkBytes.length)
              .append('\n');
    }
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /**
   * Gets the file transfer conversation id.
   *
   * @return the file transfer conversation id
   */
  public UUID getConversationId() {
    return conversationId;
  }

  /**
   * Gets the sender file path.
   *
   * @return the sender file path
   */
  public String getSenderFilePath() {
    return senderFilePath;
  }

  /**
   * Gets the recipient file path.
   *
   * @return the recipient file path
   */
  public String getRecipientFilePath() {
    return recipientFilePath;
  }

  /**
   * Gets the sender container name.
   *
   * @return the sender container name
   */
  public String getSenderContainerName() {
    return senderContainerName;
  }

  /**
   * Gets the recipient container name.
   *
   * @return the recipient container name
   */
  public String getRecipientContainerName() {
    return recipientContainerName;
  }

  /**
   * Gets the starting date time of the file transfer.
   *
   * @return the starting date time of the file transfer
   */
  public DateTime getStartingDateTime() {
    return startingDateTime;
  }

  /**
   * Gets the ending date time of the file transfer.
   *
   * @return the ending date time of the file transfer
   */
  public DateTime getEndingDateTime() {
    return endingDateTime;
  }

  /**
   * @param endingDateTime the endingDateTime to set
   */
  public void setEndingDateTime(final DateTime endingDateTime) {
    //Preconditions
    assert endingDateTime != null : "endingDateTime must not be null";

    this.endingDateTime = endingDateTime;
  }

  /**
   * Gets the hash of the transferred file's contents.
   *
   * @return the hash of the transferred file's contents
   */
  public String getFileHash() {
    return fileHash;
  }

  /**
   * Sets the hash of the transferred file's contents.
   *
   * @param fileHash the hash of the transferred file's content
   */
  public void setFileHash(final String fileHash) {
    //Preconditions
    assert StringUtils.isNonEmptyString(fileHash) : "fileHash must be a non-empty string";

    this.fileHash = fileHash;
  }

  /**
   * Gets the size of the transferred file's contents.
   *
   * @return the size of the transferred file's contents
   */
  public long getFileSize() {
    return fileSize;
  }

  /**
   * Sets the size of the transferred file's contents.
   *
   * @param fileSize the size of the transferred file's contents
   */
  public void setFileSize(final long fileSize) {
    //Preconditions
    assert fileSize >= 0 : "file size must not be negative";

    this.fileSize = fileSize;
  }

  /**
   * Gets the file transfer state.
   *
   * @return the file transfer state
   */
  public FileTransferState getFileTransferState() {
    return fileTransferState;
  }

  /**
   * Sets the file transfer state.
   *
   * @param fileTransferState the file transfer state
   */
  public void setFileTransferState(final FileTransferState fileTransferState) {
    //Preconditions
    assert fileTransferState != null : "fileTransferState must not be null";

    this.fileTransferState = fileTransferState;
  }

  /**
   * Gets the number of transferred file chunks.
   *
   * @return the number of transferred file chunks
   */
  public int getFileChunksCnt() {
    return fileChunksCnt;
  }

  /**
   * Increments the number of transferred file chunks.
   */
  public void incrementFileChunksCnt() {
    fileChunksCnt++;
  }

  /**
   * Gets the sender's file input stream.
   *
   * @return the sender's file input stream
   */
  public BufferedInputStream getBufferedInputStream() {
    return bufferedInputStream;
  }

  /**
   * Sets the sender's file input stream.
   *
   * @param bufferedInputStream the sender's file input stream
   */
  public void setBufferedInputStream(final BufferedInputStream bufferedInputStream) {
    //Preconditions
    assert bufferedInputStream != null : "bufferedInputStream must not be null";

    this.bufferedInputStream = bufferedInputStream;
  }

  /**
   * Gets the receiver's file output stream.
   *
   * @return the receiver's file output stream
   */
  public BufferedOutputStream getBufferedOutputStream() {
    return bufferedOutputStream;
  }

  /**
   * Sets the receiver's file output stream.
   *
   * @param bufferedOutputStream the receiver's file output stream
   */
  public void setBufferedOutputStream(final BufferedOutputStream bufferedOutputStream) {
    //Preconditions
    assert bufferedOutputStream != null : "bufferedOutputStream must not be null";

    this.bufferedOutputStream = bufferedOutputStream;
  }

  /** Gets the last sent file chunk bytes.
   *
   * @return the last sent file chunk bytes
   */
  public byte[] getFileChunkBytes() {
    return fileChunkBytes.clone();
  }

  /** Sets the last sent file chunk bytes.
   *
   * @param fileChunkBytes the fileChunkBytes to set
   */
  public void setFileChunkBytes(final byte[] fileChunkBytes) {
    //Preconditions
    assert fileChunkBytes != null : "fileChunkBytes must not be null";

    this.fileChunkBytes = fileChunkBytes.clone();
  }

  /** Gets the requester qualified name.
   *
   * @return the requeser qualified name
   */
  public String getRequesterQualifiedName() {
    return requesterQualifiedName;
  }

  /** Sets the requester qualified name.
   *
   * @param requestrQualifiedName the requester qualified name
   */
  public void setRequesterQualifiedName(final String requestrQualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(requestrQualifiedName) : "requesterQualifiedName must be a non-empty string";

    this.requesterQualifiedName = requestrQualifiedName;
  }

  /** Gets the requester service.
   *
   * @return the requester service
   */
  public String getRequesterService() {
    return requesterService;
  }

  /** Sets the requester service.
   *
   * @param requesterService the requester service
   */
  public void setRequesterService(final String requesterService) {
    //Preconditions
    assert StringUtils.isNonEmptyString(requesterService) : "requesterService must be a non-empty string";

    this.requesterService = requesterService;
  }

}
