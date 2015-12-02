package org.texai.photoapp;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.joda.time.DateTime;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.texai.network.netty.handler.TexaiHTTPRequestHandler;
import org.texai.network.netty.utils.NettyHTTPUtils;
import static org.texai.photoapp.S3ClientDemo.BUCKET;
import org.texai.util.Base64Coder;
import org.texai.util.HTTPUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.SymmetricKeyUtils;

/**
 * PhotoAppServer.java
 *
 * Description: Provides a websocket server for the AI Chain Photo Application.
 *
 * Copyright (C) Nov 30, 2015, Stephen L. Reed.
 */
public class PhotoAppServer implements TexaiHTTPRequestHandler {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(PhotoAppServer.class);
  /**
   * the root path
   */
  private static final File ROOT_PATH = new File("data/");
  // the users initialization object
  private final InitializedUsers initializedUsers = new InitializedUsers();
  // the user dictionary, channel --> username
  private final Map<Channel, String> userDictionary = new HashMap<>();
  // the channel dictionary, username --> channel
  private final Map<String, Channel> channelDictionary = new HashMap<>();
  // the indicator whether this is a unit test
  private boolean isUnitTest = false;

  /**
   * Creates a new instance of PhotoAppServer.
   */
  public PhotoAppServer() {
  }

  /**
   * Handles the HTTP request.
   *
   * @param httpRequest the HTTP request
   * @param channel the channel
   *
   * @return the indicator whether the HTTP request was handled
   */
  @Override
  public boolean httpRequestReceived(
          final HttpRequest httpRequest,
          final Channel channel) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert channel != null : "channel must not be null";

    LOGGER.info("httpRequest: " + httpRequest);
    LOGGER.info("method: " + httpRequest.getMethod());
    LOGGER.info("protocol version: " + httpRequest.getProtocolVersion());
    LOGGER.info("method: " + httpRequest.getMethod());
    LOGGER.info("uri: " + httpRequest.getUri());
    for (final String headerName : httpRequest.getHeaderNames()) {
      LOGGER.info("header: " + headerName + " " + httpRequest.getHeader(headerName));
    }
    final URI uri;
    try {
      uri = new URI(httpRequest.getUri());
    } catch (URISyntaxException ex) {
      throw new TexaiException(ex);
    }
    final String path = uri.getPath();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("path: " + path);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(httpRequest.getMethod() + " " + path);
      }
    }
    final Map<String, String> parameterDictionary = new HashMap<>();
    if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
      parameterDictionary.putAll(HTTPUtils.getQueryMap(uri.getRawQuery()));
    }

    try {

      final String filePath = path.replace('/', File.separatorChar);
      LOGGER.info("filePath: " + ROOT_PATH + filePath);
      if (new File(ROOT_PATH, filePath).isDirectory()) {
        NettyHTTPUtils.writeHTMLResponse(
                httpRequest,
                "<html><body><h2>You do not have permission to view this directory</h2></body></html>",
                channel,
                null); // sessionCookie
        return true;
      }

      // serve file
      File file;
      FileInputStream fileInputStream = null;
      try {
        file = new File(ROOT_PATH, filePath);
        LOGGER.info("serving file: " + file);
        fileInputStream = new FileInputStream(file);
      } catch (final FileNotFoundException ex) {
        LOGGER.info("file not found: " + filePath);
        if (fileInputStream != null) {
          fileInputStream.close();
        }
        return true;
      }
      assert file != null;
      assert fileInputStream != null;
      final byte[] chunk = new byte[(int) file.length()];
      int count;
      int pos = 0;

      while ((count = fileInputStream.read(chunk, pos, chunk.length - pos)) > 0) {
        pos += count;
      }
      if (path.endsWith(".html")) {
        NettyHTTPUtils.writeHTMLResponse(
                httpRequest,
                new String(chunk, "US-ASCII"),
                channel,
                null); // sessionCookie
      } else if (path.endsWith(".js")) {
        NettyHTTPUtils.writeTextResponse(httpRequest, new String(chunk, "US-ASCII"), channel);
      } else if (path.endsWith(".css")) {
        NettyHTTPUtils.writeCSSResponse(httpRequest, new String(chunk, "US-ASCII"), channel);
      } else {
        NettyHTTPUtils.writeBinaryResponse(
                httpRequest,
                chunk,
                channel,
                null); // sessionCookie
      }
      return true;
    } catch (final IOException ex) {
      LOGGER.error("exception message: " + ex.getMessage());
      LOGGER.error("exception: " + ex);
      LOGGER.error(StringUtils.getStackTraceAsString(ex));
      NettyHTTPUtils.writeHTMLResponse(
              httpRequest,
              "<html><body><h2>An error occured.</h2></body></html>",
              channel,
              null); // sessionCookie
      return true;
    }
  }

  /**
   * Handles a received text web socket frame.
   *
   * @param channel the channel handler context
   * @param textWebSocketFrame the text web socket frame
   *
   * @return the indicator whether the web socket request was handled
   */
  @Override
  public boolean textWebSocketFrameReceived(
          final Channel channel,
          final TextWebSocketFrame textWebSocketFrame) {
    //Preconditions
    assert channel != null : "channel must not be null";
    assert textWebSocketFrame != null : "textWebSocketFrame must not be null";

    final String webSocketText = textWebSocketFrame.getText();
    LOGGER.info("********** server receiving ************");
    LOGGER.info("web socket text received: " + webSocketText);

    final JSONParser jsonParser = new JSONParser();
    final JSONObject responseJSONObject;
    try {
      responseJSONObject = (JSONObject) jsonParser.parse(webSocketText);
    } catch (ParseException ex) {
      throw new TexaiException(ex);
    }

    final String operation = (String) responseJSONObject.get("operation");
    switch (operation) {
      case "login": {
        final String username = (String) responseJSONObject.get("username");
        loginUser(username, channel);
        break;
      }
      case "storePhoto": {
        final String photo = (String) responseJSONObject.get("photo");
        final String photoHash = (String) responseJSONObject.get("photoHash");
        storePhoto(photo, photoHash, channel);
        break;
      }
      case "sendPhoto": {
        final String photoHash = (String) responseJSONObject.get("photoHash");
        final String recipient = (String) responseJSONObject.get("recipient");
        sendPhoto(photoHash, recipient, channel);
        break;
      }
      default: {
        final String errorMessage = "invalid operation: '" + operation + "'";
        LOGGER.info(errorMessage);
        sendErrorMessage(webSocketText, channel);
      }
    }

    return true;
  }

  /**
   * Logs in the given user, and responds by provisioning the user.
   *
   * @param username the given user name
   * @param channel the channel used for the response
   */
  public void loginUser(
          final String username,
          final Channel channel) {
    if (!StringUtils.isNonEmptyString(username)) {
      sendErrorMessage(
              "username must be a non-empty string",
              channel);
      return;
    }
    if (channel == null) {
      sendErrorMessage(
              "channel must not be null",
              channel);
      return;
    }
    LOGGER.info("operation: loginUser");
    LOGGER.info("username: " + username);
    userDictionary.put(channel, username);
    channelDictionary.put(username, channel);

    LOGGER.info("********** server sending ************");
    // provisionUser
    //    {
    //      "operation": "provisionUser",
    //      "buddyList”: {"1": "Bob"}
    //    }
    final String buddyUsername;
    if (username.equals("Alice")) {
      buddyUsername = "Bob";
    } else {
      buddyUsername = "Alice";
    }
    final StringBuilder stringBuilder = new StringBuilder();
    final String jsonString = stringBuilder
            .append("{\n")
            .append("\"operation\": \"provisionUser\"\n")
            .append("\"buddyList\": {\"1\": \"")
            .append(buddyUsername)
            .append("\"}\n")
            .append("}\n")
            .toString();
    LOGGER.info("server sending: " + jsonString);
    channel.write(new TextWebSocketFrame(jsonString));
  }

  /**
   * Sends an error message to the client.
   *
   * @param errorMessage the error message
   * @param channel the channel used for the response
   */
  public void sendErrorMessage(
          final String errorMessage,
          final Channel channel) {
    //Preconditions
    assert StringUtils.isNonEmptyString(errorMessage) : "errorMessage must be a non-empty string";
    assert channel != null : "channel must not be null";

    LOGGER.info("********** server sending ************");
    // storageResponse
    //  {
    //    "operation": "errorNotification",
    //    "errorMessage": "[for example] recipient not logged in"
    //  }
    final StringBuilder stringBuilder = new StringBuilder();
    final String jsonString = stringBuilder
            .append("{\n")
            .append("\"operation\": \"errorNotification\",\n")
            .append("\"errorMessage\": \"")
            .append(errorMessage)
            .append("\"\n")
            .append("}\n")
            .toString();
    LOGGER.info("server sending: " + jsonString);
    channel.write(new TextWebSocketFrame(jsonString));
  }

  /**
   * Stores the given photo in to the Amazon S3 cloud, and responds with an acknowledgement.
   *
   * @param photo photo encoded in Base 64 notation
   * @param photoHash the SHA-1 hash of the photo encoded in Base 64 notation
   * @param channel the channel used for the response
   */
  public void storePhoto(
          final String photo,
          final String photoHash,
          final Channel channel) {
    if (!StringUtils.isNonEmptyString(photo)) {
      sendErrorMessage(
              "photo must be a non-empty string",
              channel);
      return;
    }
    if (!StringUtils.isNonEmptyString(photoHash)) {
      sendErrorMessage(
              "photoHash must be a non-empty string",
              channel);
      return;
    }
    if (channel == null) {
      throw new TexaiException("channel must not be null");
    }
    final String username = userDictionary.get(channel);
    if (username == null) {
      sendErrorMessage(
              "user not logged in",
              channel);
      return;
    }
    LOGGER.info("logged in user: " + username);
    LOGGER.info("operation: storePhoto");
    LOGGER.info("photo: " + photo);
    LOGGER.info("photoHash: " + photoHash);

    final byte[] photoBytes = Base64Coder.decode(photo);
    if (isUnitTest) {
      try {
        FileUtils.writeByteArrayToFile(new File("data/orb-test.jpg"), photoBytes);
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }

      try {
        assert FileUtils.contentEquals(new File("data/orb.jpg"), new File("data/orb-test.jpg")) : "photo files not the same content";
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }
    // encrypt the photo with a symmetric key for storage
    final File secretKeyFile = new File("data/aes-key.txt");
    SecretKey secretKey = SymmetricKeyUtils.loadKey(secretKeyFile);
    if (secretKey == null) {
      secretKey = SymmetricKeyUtils.generateKey();
      SymmetricKeyUtils.saveKey(secretKey, secretKeyFile);
    }
    LOGGER.info("photoBytes length:          " + photoBytes.length);
    final byte[] encryptedPhotoBytes = SymmetricKeyUtils.encrypt(photoBytes, secretKey);
    LOGGER.info("encryptedPhotoBytes length: " + encryptedPhotoBytes.length);

    if (!isUnitTest) {
      // store photo on Amazon S3 cloud

    }

    LOGGER.info("********** server sending ************");
    // storageResponse
    //    {
    //      "operation": "storageResponse",
    //      "timestamp": "the date and time that the photo was stored in the Amazon cloud, as a UTC string"
    //      "duplicate": "yes or no"
    //    }
    final StringBuilder stringBuilder = new StringBuilder();
    final String jsonString = stringBuilder
            .append("{\n")
            .append("\"operation\": \"storageResponse\",\n")
            .append("\"timestamp\": \"")
            .append((new DateTime()).toString())
            .append("\",\n")
            .append("\"duplicate\": \"yes\"\n")
            .append("}\n")
            .toString();
    LOGGER.info("server sending: " + jsonString);
    channel.write(new TextWebSocketFrame(jsonString));
  }

  /**
   * Sends the specified photo from the Amazon cloud to the specified buddy user. The server verifies that the photo has not been tampered
   * with.
   *
   * @param photoHash the SHA-1 hash of the photo encoded in Base 64 notation
   * @param recipient the user name of the recipient
   * @param channel the channel used for the response
   */
  public void sendPhoto(
          final String photoHash,
          final String recipient,
          final Channel channel) {
    if (!StringUtils.isNonEmptyString(photoHash)) {
      sendErrorMessage(
              "photoHash must be a non-empty string",
              channel);
      return;
    }
    if (!StringUtils.isNonEmptyString(recipient)) {
      throw new TexaiException("recipient must be a non-empty string");
    }
    if (channel == null) {
      sendErrorMessage(
              "channel must not be null",
              channel);
      return;
    }
    final String username = userDictionary.get(channel);
    if (username == null) {
      sendErrorMessage(
              "user not logged in",
              channel);
      return;
    }
    LOGGER.info("logged in user: " + username);
    LOGGER.info("operation: sendPhoto");
    LOGGER.info("photoHash: " + photoHash);
    LOGGER.info("recipient: " + recipient);

    LOGGER.info("********** server sending to " + recipient + " ************");
    //  {
    //    "operation": "receivePhoto",
    //    "photo": "photo encoded in Base 64 notation",
    //    "photoHash": "the SHA-1 hash of the selected photo encoded in Base 64 notation",
    //    "timestamp": "the date and time that the photo was stored in the Amazon cloud, as a UTC string"
    //    "sender": "Alice"
    //  }
    final Channel recipientChannel = channelDictionary.get(recipient);
    if (recipientChannel == null) {
      sendErrorMessage(
              "recipient channel must not be null",
              channel);
      return;
    }
    final StringBuilder stringBuilder = new StringBuilder();
    String jsonString = stringBuilder
            .append("{\n")
            .append("\"operation\": \"receivePhoto\",\n")
            .append("\"photo\": \"")
            .append(initializedUsers.getPhotoBase64())
            .append("\",\n")
            .append("\"photoHash\": \"")
            .append(initializedUsers.getPhotoHashBase64())
            .append("\"\n")
            .append("\"timestamp\": \"")
            .append((new DateTime()).toString())
            .append("\",\n")
            .append("\"sender\": \"")
            .append(username)
            .append("\"\n")
            .append("}\n")
            .toString();
    LOGGER.info("server sending: " + jsonString);
    recipientChannel.write(new TextWebSocketFrame(jsonString));

    LOGGER.info("********** server sending to " + username + " ************");
    //    {
    //      "operation": "photoDelivered",
    //      "photoHash": "the SHA-1 hash of the selected photo encoded in Base 64 notation",
    //      "timestamp": "the date and time that the photo was delivered, as a UTC string"
    //      "recipient": "Bob"
    //    }
    jsonString = (new StringBuilder())
            .append("{\n")
            .append("\"operation\": \"photoDelivered\",\n")
            .append("\"photoHash\": \"")
            .append(initializedUsers.getPhotoHashBase64())
            .append("\"\n")
            .append("\"timestamp\": \"")
            .append((new DateTime()).toString())
            .append("\",\n")
            .append("\"recipient\": \"")
            .append(recipient)
            .append("\"\n")
            .append("}\n")
            .toString();
    LOGGER.info("server sending: " + jsonString);
    channel.write(new TextWebSocketFrame(jsonString));
  }

  /**
   * Gets the users initialization object.
   *
   * @return the users initialization object
   */
  public InitializedUsers getInitializedUsers() {
    return initializedUsers;
  }

  /**
   * Sets the indicator whether this is a unit test.
   *
   * @param isUnitTest the indicator whether this is a unit test
   */
  protected void setIsUnitTest(final boolean isUnitTest) {
    this.isUnitTest = isUnitTest;
  }

  /**
   * Gets the uploaded object from Amazon cloud using the given key.
   *
   * @param bucket the S3 BUCKET name
   * @param key the key associated with the stored object
   *
   * @return S3Object the stored object
   */
  public S3Object getDocument(
          final String bucket,
          final String key) {
    //Preconditions
    assert StringUtils.isNonEmptyString(bucket) : "bucket must be a non-empty string";
    assert StringUtils.isNonEmptyString(key) : "key must be a non-empty string";

    final AmazonS3 s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
    S3Object objectPortion = null;
    try {
      LOGGER.info("Downloading an object with key " + key);
      S3Object s3object = s3Client.getObject(new GetObjectRequest(
              bucket, key));
      LOGGER.info("Content-Type: "
              + s3object.getObjectMetadata().getContentType());

      // Get a range of bytes from an object.
      GetObjectRequest rangeObjectRequest = new GetObjectRequest(
              bucket, key);
      rangeObjectRequest.setRange(0, 10);
      objectPortion = s3Client.getObject(rangeObjectRequest);

      LOGGER.info(objectPortion.getKey() + "=" + objectPortion.getObjectMetadata().getInstanceLength());
    } catch (AmazonServiceException ase) {
      LOGGER.info("Caught an AmazonServiceException, which"
              + " means your request made it "
              + "to Amazon S3, but was rejected with an error response"
              + " for some reason.");
      LOGGER.info("Error Message:    " + ase.getMessage());
      LOGGER.info("HTTP Status Code: " + ase.getStatusCode());
      LOGGER.info("AWS Error Code:   " + ase.getErrorCode());
      LOGGER.info("Error Type:       " + ase.getErrorType());
      LOGGER.info("Request ID:       " + ase.getRequestId());
    } catch (AmazonClientException ace) {
      LOGGER.info("Caught an AmazonClientException, which means"
              + " the client encountered "
              + "an internal error while trying to "
              + "communicate with S3, "
              + "such as not being able to access the network.");
      LOGGER.info("Error Message: " + ace.getMessage());
    }
    return objectPortion;
  }

  /**
   * lists the bucket objects.
   */
  public void listBucketObjects() {
    AmazonS3 s3Client = new AmazonS3Client(new ProfileCredentialsProvider());

    try {
      LOGGER.info("Listing objects Start");

      final ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
              .withBucketName(BUCKET)
              .withDelimiter("/");
      ObjectListing objectListing;
      do {
        objectListing = s3Client.listObjects(listObjectsRequest);
        for (S3ObjectSummary objectSummary
                : objectListing.getObjectSummaries()) {
          LOGGER.info(" - " + objectSummary.getKey() + "  "
                  + "(size = " + objectSummary.getSize()
                  + ")");
        }
        listObjectsRequest.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());
    } catch (AmazonServiceException ase) {
      LOGGER.info("Caught an AmazonServiceException, "
              + "which means your request made it "
              + "to Amazon S3, but was rejected with an error response "
              + "for some reason.");
      LOGGER.info("Error Message:    " + ase.getMessage());
      LOGGER.info("HTTP Status Code: " + ase.getStatusCode());
      LOGGER.info("AWS Error Code:   " + ase.getErrorCode());
      LOGGER.info("Error Type:       " + ase.getErrorType());
      LOGGER.info("Request ID:       " + ase.getRequestId());
    } catch (AmazonClientException ace) {
      LOGGER.info("Caught an AmazonClientException, "
              + "which means the client encountered "
              + "an internal error while trying to communicate"
              + " with S3, "
              + "such as not being able to access the network.");
      LOGGER.info("Error Message: " + ace.getMessage());
    }
    LOGGER.info("Listing objects End");
  }

  /**
   * uploadObject - uploads a file to Amazon cloud based on key and BUCKET.
   *
   * @param bucket the S3 bucket
   * @param key the object key
   * @param filePath the file path
   */
  public void uploadObject(
          final String bucket,
          final String key,
          final String filePath) {
    AmazonS3 s3Client = new AmazonS3Client(new ProfileCredentialsProvider());

    // Create a list of UploadPartResponse objects. You get one of these for
    // each part upload.
    final List<PartETag> partETags = new ArrayList<>();

    // Step 1: Initialize.
    final InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucket, key);
    final InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

    final File file = new File(filePath);
    final long contentLength = file.length();
    long partSize = 5 * 1024 * 1024; // Set part size to 5 MB.

    try {
      // Step 2: Upload parts.
      long filePosition = 0;
      for (int i = 1; filePosition < contentLength; i++) {
        // Last part can be less than 5 MB. Adjust part size.
        partSize = Math.min(partSize, (contentLength - filePosition));

        // Create request to upload a part.
        UploadPartRequest uploadRequest = new UploadPartRequest()
                .withBucketName(bucket).withKey(key)
                .withUploadId(initResponse.getUploadId()).withPartNumber(i)
                .withFileOffset(filePosition)
                .withFile(file)
                .withPartSize(partSize);

        // Upload part and add response to our list.
        partETags.add(s3Client.uploadPart(uploadRequest).getPartETag());

        filePosition += partSize;
      }

      // Step 3: Complete.
      final CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(
              bucket,
              key,
              initResponse.getUploadId(),
              partETags);

      s3Client.completeMultipartUpload(compRequest);
    } catch (Exception e) {
      s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, initResponse.getUploadId()));
    }
  }
}
