package com.example.demo.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class SecurityInterceptor(private val jwtService: JwtService) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // Exclude pre-flight CORS OPTIONS requests from authorization checks!
        if (request.method == "OPTIONS") {
            return true
        }

        if (handler !is HandlerMethod) return true

        // Always parse Authorization token if present so controllers can access user context
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            val claims = jwtService.validateTokenAndGetClaims(token)
            if (claims != null) {
                request.setAttribute("currentUserEmail", claims["email"] as? String)
                request.setAttribute("currentUserRole", claims["role"] as? String)
            }
        }

        // Check if the handler method has a @RequiresRole annotation
        val requiresRole = handler.getMethodAnnotation(RequiresRole::class.java)
            ?: handler.beanType.getAnnotation(RequiresRole::class.java)
            ?: return true // No annotation, endpoint is public!

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = "application/json"
            response.writer.write("{\"error\": \"Unauthorized: Missing or malformed Authorization header. Please log in first.\"}")
            return false
        }

        val token = authHeader.substring(7)
        val claims = jwtService.validateTokenAndGetClaims(token)
        if (claims == null) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = "application/json"
            response.writer.write("{\"error\": \"Unauthorized: Invalid or expired JWT token. Please log in again.\"}")
            return false
        }

        val userRole = claims["role"] as? String
        val userEmail = claims["email"] as? String

        if (!requiresRole.value.contains(userRole)) {
            response.status = HttpStatus.FORBIDDEN.value()
            response.contentType = "application/json"
            response.writer.write("{\"error\": \"Forbidden: Access denied. This endpoint requires one of the following roles: ${requiresRole.value.joinToString(", ")}.\"}")
            return false
        }

        // Put the claims into the request attributes
        request.setAttribute("currentUserEmail", userEmail)
        request.setAttribute("currentUserRole", userRole)

        return true
    }
}
