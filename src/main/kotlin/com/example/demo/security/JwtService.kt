package com.example.demo.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class JwtService(
    @Value("\${jwt.secret}")
    private val secret: String
) {

    private val base64Encoder = Base64.getUrlEncoder().withoutPadding()
    private val base64Decoder = Base64.getUrlDecoder()
    private val mapper = JsonMapper.builder().build()

    fun generateToken(email: String, role: String): String {
        val header = mapOf("alg" to "HS256", "typ" to "JWT")
        val payload = mapOf(
            "email" to email,
            "role" to role,
            "exp" to (System.currentTimeMillis() + 3600000) // 1 hour expiration
        )

        val headerJson = mapper.writeValueAsString(header)
        val payloadJson = mapper.writeValueAsString(payload)

        val headerBase64 = base64Encoder.encodeToString(headerJson.toByteArray(StandardCharsets.UTF_8))
        val payloadBase64 = base64Encoder.encodeToString(payloadJson.toByteArray(StandardCharsets.UTF_8))

        val signatureInput = "$headerBase64.$payloadBase64"
        val signature = hmacSha256(signatureInput, secret)
        val signatureBase64 = base64Encoder.encodeToString(signature)

        return "$signatureInput.$signatureBase64"
    }

    fun validateTokenAndGetClaims(token: String): Map<String, Any>? {
        val parts = token.split(".")
        if (parts.size != 3) return null

        val headerBase64 = parts[0]
        val payloadBase64 = parts[1]
        val signatureBase64 = parts[2]

        // Verify signature
        val signatureInput = "$headerBase64.$payloadBase64"
        val calculatedSignature = hmacSha256(signatureInput, secret)
        val calculatedSignatureBase64 = base64Encoder.encodeToString(calculatedSignature)

        if (signatureBase64 != calculatedSignatureBase64) {
            return null // Token tampered!
        }

        // Parse payload
        val payloadJson = String(base64Decoder.decode(payloadBase64), StandardCharsets.UTF_8)
        val payload = mapper.readValue(payloadJson, Map::class.java) as Map<String, Any>

        // Check expiration
        val exp = payload["exp"] as? Number ?: return null
        if (System.currentTimeMillis() > exp.toLong()) {
            return null // Token expired!
        }

        return payload
    }

    private fun hmacSha256(input: String, secret: String): ByteArray {
        val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        return mac.doFinal(input.toByteArray(StandardCharsets.UTF_8))
    }
}
