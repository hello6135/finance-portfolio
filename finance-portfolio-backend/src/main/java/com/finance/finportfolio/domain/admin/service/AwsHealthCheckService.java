package com.finance.finportfolio.domain.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.HashMap;
import java.util.Map;

@Service
public class AwsHealthCheckService {

    private final S3Client s3Client;
    private final Ec2Client ec2Client;

    private static final String STATUS = "status";
    private static final String MESSAGE = "message";

    @Value("${spring.cloud.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${spring.cloud.aws.ec2.instance-id}")
    private String instanceId;

    public AwsHealthCheckService(S3Client s3Client, Ec2Client ec2Client) {
        this.s3Client = s3Client;
        this.ec2Client = ec2Client;
    }

    public Map<String, Object> checkAwsStatus() {
        Map<String, Object> statusMap = new HashMap<>();

        statusMap.put("s3", checkS3Status());
        statusMap.put("ec2", checkEc2Status());

        return statusMap;
    }

    private Map<String, Object> checkS3Status() {
        Map<String, Object> s3Result = new HashMap<>();
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.headBucket(headBucketRequest);
            s3Result.put(STATUS, "UP");
            s3Result.put(MESSAGE, "S3 Bucket access successful.");
        } catch (S3Exception e) {
            s3Result.put(STATUS, "DOWN");
            s3Result.put(MESSAGE, "S3 Error: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            s3Result.put(STATUS, "DOWN");
            s3Result.put(MESSAGE, "Unexpected Error: " + e.getMessage());
        }
        return s3Result;
    }

    private Map<String, Object> checkEc2Status() {
        Map<String, Object> ec2Result = new HashMap<>();
        try {
            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            DescribeInstancesResponse response = ec2Client.describeInstances(request);

            // 인스턴스의 현재 상태 추출 (e.g., running, stopped)
            String state = response.reservations().get(0)
                    .instances().get(0)
                    .state()
                    .name()
                    .toString();

            ec2Result.put(STATUS, "UP");
            ec2Result.put("instanceState", state);
        } catch (Ec2Exception e) {
            ec2Result.put(STATUS, "DOWN");
            ec2Result.put(MESSAGE, "EC2 Error: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            ec2Result.put(STATUS, "DOWN");
            ec2Result.put(MESSAGE, "Unexpected Error: " + e.getMessage());
        }
        return ec2Result;
    }
}
