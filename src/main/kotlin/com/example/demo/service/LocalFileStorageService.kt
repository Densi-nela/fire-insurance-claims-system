package com.example.demo.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["file.storage.type"], havingValue = "local", matchIfMissing = true)
class LocalFileStorageService : FileStorageService {

    private val uploadDirectory = "./uploads"

    init {
        val uploadPath = Paths.get(uploadDirectory)
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath)
            println("[LocalFileStorage] Created local uploads directory at: ${uploadPath.toAbsolutePath()}")
        }
    }

    override fun store(inputStream: InputStream, originalFileName: String, contentType: String): String {
        val uniqueFileName = "${UUID.randomUUID()}_$originalFileName"
        val targetPath = Paths.get(uploadDirectory, uniqueFileName)
        
        // Write the input stream bytes to disk
        Files.copy(inputStream, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        println("[LocalFileStorage] Saved file to disk: $targetPath")
        
        return targetPath.toString()
    }

    override fun loadAsResource(filePath: String): Resource {
        val file = File(filePath)
        if (!file.exists()) {
            throw RuntimeException("File not found on local disk: $filePath")
        }
        return FileSystemResource(file)
    }

    override fun delete(filePath: String) {
        Files.deleteIfExists(Paths.get(filePath))
        println("[LocalFileStorage] Deleted file from disk: $filePath")
    }
}
