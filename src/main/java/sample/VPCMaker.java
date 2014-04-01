package sample;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.CreateVpcResult;
import com.amazonaws.services.ec2.model.Vpc;

public class VPCMaker {
    public static Vpc createVPC(AmazonEC2Client client, String cidr) {
        CreateVpcResult vpcResult = client.createVpc(
                new CreateVpcRequest(cidr)
        );

        return vpcResult.getVpc();
    }

    public static void main(String[] args) throws Exception {

        ClientConfiguration config = new ClientConfiguration()
                .withProxyHost("http.proxy.fmr.com")
                .withProxyPort(8000);

        AmazonEC2Client client = new AmazonEC2Client(
                new ClasspathPropertiesFileCredentialsProvider(), config
        );

        Vpc vpc = createVPC(client, "10.0.0.0/16");
        System.out.println(vpc.toString());

    }
}
