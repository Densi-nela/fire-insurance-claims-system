package com.example.demo.service

import org.springframework.core.io.Resource
import java.io.InputStream

interface FileStorageService {
    fun store(inputStream: InputStream, originalFileName: String, contentType: String): String
    fun loadAsResource(filePath: String): Resource
    fun delete(filePath: String)
}
