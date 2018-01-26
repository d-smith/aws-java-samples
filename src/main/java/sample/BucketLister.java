package sample;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;

public class BucketLister {
    public static void main(String[] args) throws Exception {

        ClientConfiguration config = new ClientConfiguration()
                .withProxyHost("proxy-host")
                .withProxyPort(1234);

       AmazonS3 s3 = new AmazonS3Client(new ClasspathPropertiesFileCredentialsProvider(), config);

        System.out.println("Listing buckets");
        for (Bucket bucket : s3.listBuckets()) {
            System.out.println(" - " + bucket.getName());
        }
    }
}
