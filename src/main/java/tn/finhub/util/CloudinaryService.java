package tn.finhub.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CloudinaryService {

    private static Cloudinary cloudinary;

    static {
        Dotenv dotenv = Dotenv.load();
        String cloudinaryUrl = dotenv.get("CLOUDINARY_URL");
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            throw new RuntimeException("CLOUDINARY_URL not found in .env file");
        }
        cloudinary = new Cloudinary(cloudinaryUrl);
    }

    public static String upload(File file) {
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    public static String upload(File file, String folder) {
        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto");
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file, params);
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }
}
