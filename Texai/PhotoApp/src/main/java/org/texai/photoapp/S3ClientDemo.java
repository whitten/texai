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
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.texai.util.StringUtils;

/**
 * S3ClientDemo.java
 *
 * Description:
 *
 * Copyright (C) Dec 2, 2015, Stephen L. Reed.
 */
public class S3ClientDemo {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(S3ClientDemo.class);
  // the Amazon S3 bucket
  public static final String BUCKET = "aichain-demo";

  public static void main(String[] args) {
    final S3ClientDemo s3ClientDemo = new S3ClientDemo();
    String key = "12345";
    String filePath = "data/orb.jpg";
    s3ClientDemo.uploadObject(BUCKET, key, filePath);

    s3ClientDemo.listBucketObjects();

    S3Object testObject = s3ClientDemo.getDocument(BUCKET, key);
    LOGGER.info(testObject.getKey() + "=" + testObject.getObjectMetadata().getInstanceLength());
  }

  /**
   * Creates a new S3ClientDemo instance.
   */
  public S3ClientDemo() {
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
    //Preconditions
    assert StringUtils.isNonEmptyString(bucket) : "bucket must be a non-empty string";
    assert StringUtils.isNonEmptyString(key) : "key must be a non-empty string";
    assert StringUtils.isNonEmptyString(filePath) : "filePath must be a non-empty string";

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
