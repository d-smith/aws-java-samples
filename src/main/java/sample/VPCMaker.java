package sample;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

import java.util.ArrayList;
import java.util.Collection;

public class VPCMaker {
    private AmazonEC2Client client;

    public VPCMaker() {
        ClientConfiguration config = new ClientConfiguration()
                ;

        client = new AmazonEC2Client(
                new ClasspathPropertiesFileCredentialsProvider(), config
        );
    }

    public Vpc createVPC(String cidr) {
        CreateVpcResult vpcResult = client.createVpc(
                new CreateVpcRequest(cidr)
        );

        return vpcResult.getVpc();
    }

    public Subnet createSubnet(String vpcId, String cidr, String az) {
        CreateSubnetResult subnetResult = client.createSubnet(
                new CreateSubnetRequest(vpcId, cidr)
                        .withAvailabilityZone(az)
        );

        return subnetResult.getSubnet();
    }

    public String createAndAttachInternetGatewayToVPC(Vpc vpc) {
        CreateInternetGatewayResult igwResult = client.createInternetGateway();
        client.attachInternetGateway(
                new AttachInternetGatewayRequest()
                        .withVpcId(vpc.getVpcId())
                        .withInternetGatewayId(igwResult.getInternetGateway().getInternetGatewayId())
        );

        return igwResult.getInternetGateway().getInternetGatewayId();
    }


    public RouteTable createRouteTableForVPC(Vpc vpc) {
        CreateRouteTableResult rtr = client.createRouteTable(
                new CreateRouteTableRequest().withVpcId(vpc.getVpcId())
        );

        return rtr.getRouteTable();
    }

    public void createRouteToGateway(String routeTableId, String cidr, String gatewayId) {
        client.createRoute(
                new CreateRouteRequest()
                        .withRouteTableId(routeTableId)
                        .withDestinationCidrBlock(cidr)
                        .withGatewayId(gatewayId)
        );
    }

    public void associateRouteTableWithSubnet(String routeTableId, String subnetId) {
        client.associateRouteTable(
                new AssociateRouteTableRequest()
                    .withRouteTableId(routeTableId)
                    .withSubnetId(subnetId)
        );
    }

    public String createSecurityGroup(String groupName, String desc, String vpcId) {
        CreateSecurityGroupResult result =  client.createSecurityGroup(
                new CreateSecurityGroupRequest()
                    .withGroupName(groupName)
                    .withDescription(desc)
                    .withVpcId(vpcId)
        );

        return result.getGroupId();
    }

    public void allowSshIngress(String groupId, String ipRange) {
       AuthorizeSecurityGroupIngressRequest authIngress = new AuthorizeSecurityGroupIngressRequest();
        IpPermission sshPerms = new IpPermission()
                .withIpRanges(ipRange)
                .withIpProtocol("tcp")
                .withFromPort(22)
                .withToPort(22);

        Collection<IpPermission> ipPerms = new ArrayList<IpPermission>();
        ipPerms.add(sshPerms);

        authIngress.setGroupId(groupId);
        authIngress.setIpPermissions(ipPerms);


        client.authorizeSecurityGroupIngress(authIngress);

    }

    public void allowSShIngressFromGroup(String sourceGroupId, String groupId) {
        AuthorizeSecurityGroupIngressRequest authIngress = new AuthorizeSecurityGroupIngressRequest();
        authIngress.setGroupId(groupId);
        authIngress.setIpProtocol("tcp");
        authIngress.setFromPort(22);
        authIngress.setToPort(22);
        authIngress.setSourceSecurityGroupOwnerId(sourceGroupId);


        client.authorizeSecurityGroupIngress(authIngress);
    }


    public static void main(String[] args) throws Exception {

        VPCMaker vpcMaker = new VPCMaker();

        //
        //Create public subnet and route to the outside via internet gateway and routing table
        //
        Vpc vpc = vpcMaker.createVPC("10.0.0.0/16");
        System.out.println(vpc.toString());

        Subnet subnet = vpcMaker.createSubnet(vpc.getVpcId(), "10.0.0.0/24", "us-east-1d");
        System.out.println(subnet);

        String gatewayId = vpcMaker.createAndAttachInternetGatewayToVPC(vpc);
        RouteTable routeTable = vpcMaker.createRouteTableForVPC(vpc);
        System.out.println(routeTable);

        vpcMaker.createRouteToGateway(routeTable.getRouteTableId(), "0.0.0.0/0", gatewayId);
        vpcMaker.associateRouteTableWithSubnet(routeTable.getRouteTableId(), subnet.getSubnetId());

        //
        // Create a private subnet
        //
        Subnet privateSubnet = vpcMaker.createSubnet(vpc.getVpcId(), "10.0.1.0/24", "us-east-1c");

        //
        // Create a security group allowing ssh inbound from anywhere
        //
        String groupName = "launch-sg";
        String launchGroupId = vpcMaker.createSecurityGroup(groupName, "vpc-launch-sg", vpc.getVpcId());
        System.out.println("create security group " + launchGroupId);

        vpcMaker.allowSshIngress(launchGroupId, "0.0.0.0/0");

        //
        // Create a security group allowing inbound access from VPC
        //
        groupName = "route-to-private-subnet-sg";
        String routeToSubnetGroupId = vpcMaker.createSecurityGroup(groupName, groupName, vpc.getVpcId());
        System.out.println("created security group: " + routeToSubnetGroupId);

        //vpcMaker.allowSShIngressFromGroup(launchGroupId, routeToSubnetGroupId);
        vpcMaker.allowSshIngress(routeToSubnetGroupId, "10.0.0.0/16");


    }


}
