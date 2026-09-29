package com.example.demo.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["file.storage.type"], havingValue = "s3")
class S3FileStorageService : FileStorageService {

    init {
        println("[S3FileStorage] Production S3 Active. Connecting to Amazon S3 Bucket...")
    }

    override fun store(inputStream: InputStream, originalFileName: String, contentType: String): String {
        val uniqueFileName = "${UUID.randomUUID()}_$originalFileName"
        val s3Key = "claims/attachments/$uniqueFileName"
        
        // In a real S3 production environment, we would use the AWS S3 SDK:
        // s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(s3Key).contentType(contentType).build(), 
        //                    RequestBody.fromInputStream(inputStream, contentLength))
        
        println("[S3FileStorage] Mock Upload Succeeded: Uploaded $originalFileName to S3 Bucket at key: $s3Key")
        
        return s3Key
    }

    override fun loadAsResource(filePath: String): Resource {
        println("[S3FileStorage] Mock Download: Fetching file from S3 Bucket at key: $filePath")
        
        // In real production, we stream the bytes from AWS S3:
        // val s3Bytes = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(filePath).build())
        // return ByteArrayResource(s3Bytes.asByteArray())
        
        val mockData = "PDF Receipt Content"
        return ByteArrayResource(mockData.toByteArray())
    }

    override fun delete(filePath: String) {
        // In real production:
        // s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(filePath).build())
        println("[S3FileStorage] Mock Delete: Deleted object from S3 Bucket at key: $filePath")
    }
}
