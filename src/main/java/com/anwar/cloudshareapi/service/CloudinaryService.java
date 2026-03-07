package com.anwar.cloudshareapi.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * CloudinaryService handles all file operations with Cloudinary cloud storage
 * Provides upload, delete, and URL generation capabilities
 * 
 * Industry-standard cloud file storage integration
 * - No local file storage required
 * - Automatic CDN distribution
 * - Built-in optimization and transformations
 * - Secure file management
 */
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    @Value("${cloudinary.cloud.name}")
    private String cloudName;

    @Value("${cloudinary.api.key}")
    private String apiKey;

    @Value("${cloudinary.api.secret}")
    private String apiSecret;

    @Value("${cloudinary.upload.folder}")
    private String uploadFolder;

    /**
     * Initialize Cloudinary instance with configured credentials
     * This is called internally before any Cloudinary operation
     */
    private Cloudinary getCloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    /**
     * Upload a file to Cloudinary
     * 
     * @param file The MultipartFile to upload
     * @param clerkId User's clerk ID (used for file organization)
     * @return Map containing upload response with secureUrl and publicId
     * @throws IOException if file upload fails
     */
    public Map<String, Object> uploadFile(MultipartFile file, String clerkId) throws IOException {
        try {
            Cloudinary cloudinary = getCloudinary();

            // Prepare folder structure: cloudshare/userId/timestamp
            String folder = uploadFolder + "/" + clerkId + "/" + System.currentTimeMillis();

            // Prepare upload parameters
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("folder", folder);
            uploadParams.put("resource_type", "auto"); // Auto-detect file type
            uploadParams.put("use_filename", true);
            uploadParams.put("unique_filename", true); // Ensure unique filenames
            uploadParams.put("overwrite", false);

            // Upload file to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            // Extract relevant information from response
            Map<String, Object> response = new HashMap<>();
            response.put("cloudinaryUrl", uploadResult.get("secure_url")); // HTTPS URL
            response.put("publicId", uploadResult.get("public_id")); // Unique identifier
            response.put("size", uploadResult.get("bytes")); // File size in bytes
            response.put("fileType", uploadResult.get("resource_type")); // File type
            response.put("format", uploadResult.get("format")); // File format (jpg, pdf, etc)
            response.put("cloudinaryPublicId", uploadResult.get("public_id")); // For deletion later

            System.out.println("✅ File uploaded to Cloudinary - Public ID: " + uploadResult.get("public_id"));

            return response;

        } catch (IOException e) {
            System.err.println("❌ Error uploading file to Cloudinary: " + e.getMessage());
            throw new IOException("Failed to upload file to cloud storage: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a file from Cloudinary using its public ID
     * 
     * @param publicId The Cloudinary public ID of the file to delete
     * @throws IOException if file deletion fails
     */
    public void deleteFile(String publicId) throws IOException {
        try {
            if (publicId == null || publicId.trim().isEmpty()) {
                System.warn("⚠️  Public ID is empty, skipping deletion");
                return;
            }

            Cloudinary cloudinary = getCloudinary();

            Map<String, Object> deleteParams = new HashMap<>();
            deleteParams.put("resource_type", "auto");

            Map deleteResult = cloudinary.uploader().destroy(publicId, deleteParams);

            String result = (String) deleteResult.get("result");

            if ("ok".equals(result)) {
                System.out.println("✅ File deleted from Cloudinary - Public ID: " + publicId);
            } else if ("not found".equals(result)) {
                System.out.println("⚠️  File not found in Cloudinary - Public ID: " + publicId);
            } else {
                System.err.println("⚠️  Unexpected deletion result: " + result);
            }

        } catch (Exception e) {
            System.err.println("⚠️  Error deleting file from Cloudinary: " + e.getMessage());
            // Don't throw - file might already be deleted or public ID invalid
            // Database record will still be deleted, file is just orphaned
        }
    }

    /**
     * Generate a transformable Cloudinary URL for the file
     * Useful for dynamic transformations like resizing, quality adjustment, etc.
     * 
     * @param publicId The Cloudinary public ID
     * @return Transformation-enabled URL
     */
    public String getTransformableUrl(String publicId) {
        try {
            Cloudinary cloudinary = getCloudinary();
            return cloudinary.url().secure(true).generate(publicId);
        } catch (Exception e) {
            System.err.println("❌ Error generating transformable URL: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verify if file exists in Cloudinary
     * 
     * @param publicId The Cloudinary public ID
     * @return true if file exists, false otherwise
     */
    public boolean fileExists(String publicId) {
        try {
            Cloudinary cloudinary = getCloudinary();
            Map<String, Object> result = cloudinary.api().resource(publicId, ObjectUtils.asMap(
                    "type", "upload"
            ));
            return result != null && result.containsKey("public_id");
        } catch (Exception e) {
            // Resource not found or API error
            return false;
        }
    }
}
