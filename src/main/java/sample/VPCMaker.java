package sample;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

public class VPCMaker {

    public static Vpc createVPC(AmazonEC2Client client, String cidr) {
        CreateVpcResult vpcResult = client.createVpc(
                new CreateVpcRequest(cidr)
        );

        return vpcResult.getVpc();
    }

    public static Subnet createSubnet(AmazonEC2Client client, String vpcId, String cidr, String az) {
        CreateSubnetResult subnetResult = client.createSubnet(
                new CreateSubnetRequest(vpcId, cidr)
                        .withAvailabilityZone(az)
        );

        return subnetResult.getSubnet();
    }

    public static String createAndAttachInternetGatewayToVPC(AmazonEC2Client client, Vpc vpc) {
        CreateInternetGatewayResult igwResult = client.createInternetGateway();
        client.attachInternetGateway(
                new AttachInternetGatewayRequest()
                        .withVpcId(vpc.getVpcId())
                        .withInternetGatewayId(igwResult.getInternetGateway().getInternetGatewayId())
        );

        return igwResult.getInternetGateway().getInternetGatewayId();
    }


    public static RouteTable createRouteTableForVPC(AmazonEC2Client client, Vpc vpc) {
        CreateRouteTableResult rtr = client.createRouteTable(
                new CreateRouteTableRequest().withVpcId(vpc.getVpcId())
        );

        return rtr.getRouteTable();
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

        Subnet subnet = createSubnet(client, vpc.getVpcId(), "10.0.0.0/24", "us-east-1d");
        System.out.println(subnet);

        String gatewayId = createAndAttachInternetGatewayToVPC(client, vpc);

        RouteTable routeTable = createRouteTableForVPC(client, vpc);

        System.out.println(routeTable);


        client.createRoute(
                new CreateRouteRequest()
                    .withRouteTableId(routeTable.getRouteTableId())
                    .withDestinationCidrBlock("0.0.0.0/0")
                    .withGatewayId(gatewayId)
        );

        AssociateRouteTableResult associateRouteTableResult = client.associateRouteTable(
            new AssociateRouteTableRequest()
                .withRouteTableId(routeTable.getRouteTableId())
                .withSubnetId(subnet.getSubnetId())
        );

        System.out.println("association id (route table to subnet): " + associateRouteTableResult.getAssociationId());

    }
}
