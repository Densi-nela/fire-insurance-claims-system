package com.example.demo.controller

import com.example.demo.model.Customer
import com.example.demo.model.Policy
import com.example.demo.model.Claim
import com.example.demo.model.Attachment
import com.example.demo.repository.AttachmentRepository
import com.example.demo.repository.ClaimRepository
import com.example.demo.repository.CustomerRepository
import com.example.demo.repository.PolicyRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import tools.jackson.databind.ObjectMapper
import com.example.demo.security.JwtService
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AttachmentControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var policyRepository: PolicyRepository

    @Autowired
    private lateinit var claimRepository: ClaimRepository

    @Autowired
    private lateinit var attachmentRepository: AttachmentRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jwtService: JwtService

    private val customerToken: String by lazy {
        "Bearer " + jwtService.generateToken("michael.scott@dundermifflin.com", "CUSTOMER")
    }

    private val testUploads = mutableListOf<String>()

    @AfterEach
    fun cleanUpFiles() {
        // Clean up any files created on disk during tests
        testUploads.forEach { filePath ->
            try {
                Files.deleteIfExists(Paths.get(filePath))
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun createTestClaim(): Claim {
        val uniqueEmail = "test.${UUID.randomUUID()}@example.com"
        val uniquePolicyNumber = "POL-TEST-${UUID.randomUUID()}"
        
        val customer = Customer(
            name = "John Doe",
            email = uniqueEmail,
            phoneNumber = "555-0100"
        )
        val savedCustomer = customerRepository.save(customer)

        val policy = Policy(
            policyNumber = uniquePolicyNumber,
            propertyAddress = "123 Mockingbird Ln",
            coverageLimit = 100000.0,
            customer = savedCustomer
        )
        val savedPolicy = policyRepository.save(policy)

        val claim = Claim(
            causeOfFire = "Electrical short in kitchen",
            estimatedPropertyDamage = 5000.0,
            estimatedContentDamage = 2000.0,
            isLivable = true,
            status = "SUBMITTED",
            policy = savedPolicy
        )
        return claimRepository.save(claim)
    }

    @Test
    fun `POST api claims attachments uploads supporting file successfully`() {
        val claim = createTestClaim()
        val mockFile = MockMultipartFile(
            "file",
            "damage_photo.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "mock-image-bytes-here".toByteArray()
        )

        val result = mockMvc.perform(
            multipart("/api/claims/${claim.id}/attachments")
                .file(mockFile)
                .header("Authorization", customerToken)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.fileName").value("damage_photo.jpg"))
            .andExpect(jsonPath("$.fileType").value(MediaType.IMAGE_JPEG_VALUE))
            .andExpect(jsonPath("$.downloadUrl").exists())
            .andReturn()

        // Track the saved file for cleanup
        val responseMap = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val attachmentId = (responseMap["id"] as Number).toLong()
        val attachment = attachmentRepository.findById(attachmentId).get()
        testUploads.add(attachment.filePath)
    }

    @Test
    fun `POST api claims attachments returns 404 for non-existent claim`() {
        val mockFile = MockMultipartFile(
            "file",
            "damage_photo.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "mock-image-bytes-here".toByteArray()
        )

        mockMvc.perform(
            multipart("/api/claims/999999/attachments")
                .file(mockFile)
                .header("Authorization", customerToken)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error", containsString("Claim not found")))
    }

    @Test
    fun `GET api attachments returns physical file cleanly`() {
        val claim = createTestClaim()
        
        // Write a temp file to simulate physical upload
        val tempUniqueName = "test_${UUID.randomUUID()}_receipt.pdf"
        val tempPath = Paths.get("./uploads", tempUniqueName)
        val mockContent = "PDF Receipt Content"
        Files.write(tempPath, mockContent.toByteArray())
        testUploads.add(tempPath.toString())

        // Save metadata to database
        val attachment = Attachment(
            fileName = "receipt.pdf",
            fileType = MediaType.APPLICATION_PDF_VALUE,
            filePath = tempPath.toString(),
            fileSize = mockContent.length.toLong(),
            claim = claim
        )
        val savedAttachment = attachmentRepository.save(attachment)

        mockMvc.perform(get("/api/attachments/${savedAttachment.id}"))
            .andExpect(status().isOk)
            .andExpect { result ->
                val body = result.response.contentAsString
                assert(body == mockContent)
                assert(result.response.contentType == MediaType.APPLICATION_PDF_VALUE)
                assert(result.response.getHeader("Content-Disposition") == "inline; filename=\"receipt.pdf\"")
            }
    }

    @Test
    fun `GET api attachments returns 404 if file does not exist on disk`() {
        val claim = createTestClaim()

        // Save metadata referencing a non-existent physical file
        val attachment = Attachment(
            fileName = "missing_file.png",
            fileType = MediaType.IMAGE_PNG_VALUE,
            filePath = "./uploads/this_file_does_not_exist_at_all.png",
            fileSize = 100L,
            claim = claim
        )
        val savedAttachment = attachmentRepository.save(attachment)

        mockMvc.perform(get("/api/attachments/${savedAttachment.id}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error", containsString("File not found on local disk")))
    }
}
