package com.finance.finportfolio.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class AwsHealthCheckServiceTest {

        @Mock
        private S3Client s3Client;

        @Mock
        private Ec2Client ec2Client;

        @InjectMocks
        private AwsHealthCheckService awsHealthCheckService;

        private static final String TEST_BUCKET = "test-s3-bucket";
        private static final String TEST_INSTANCE_ID = "i-0123456789abcdef0";

        @BeforeEach
        void setUp() {
                // @Value 어노테이션으로 주입되는 필드 값 설정
                ReflectionTestUtils.setField(awsHealthCheckService, "bucketName", TEST_BUCKET);
                ReflectionTestUtils.setField(awsHealthCheckService, "instanceId", TEST_INSTANCE_ID);
        }

        @Test
        @DisplayName("S3 및 EC2 상태가 정상일 때 전체 헬스체크 성공")
        void checkAwsStatus_Success() {
                // given
                // 1. S3 Mocking: headBucket 호출 시 예외 없이 정상 종료되도록 설정
                given(s3Client.headBucket(any(HeadBucketRequest.class)))
                                .willReturn(HeadBucketResponse.builder().build());

                // 2. EC2 Mocking: 구조화된 가짜 Response 생성
                InstanceState instanceState = InstanceState.builder()
                                .name(InstanceStateName.RUNNING)
                                .build();

                software.amazon.awssdk.services.ec2.model.Instance instance = software.amazon.awssdk.services.ec2.model.Instance
                                .builder()
                                .state(instanceState)
                                .build();

                Reservation reservation = Reservation.builder()
                                .instances(Collections.singletonList(instance))
                                .build();

                DescribeInstancesResponse mockEc2Response = DescribeInstancesResponse.builder()
                                .reservations(Collections.singletonList(reservation))
                                .build();

                given(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                                .willReturn(mockEc2Response);

                // when
                Map<String, Object> result = awsHealthCheckService.checkAwsStatus();

                // then
                assertThat(result).containsKey("s3").containsKey("ec2");

                @SuppressWarnings("unchecked")
                Map<String, Object> s3Result = (Map<String, Object>) result.get("s3");
                assertThat(s3Result)
                                .containsEntry("status", "UP")
                                .containsEntry("message", "S3 Bucket access successful.");

                @SuppressWarnings("unchecked")
                Map<String, Object> ec2Result = (Map<String, Object>) result.get("ec2");
                assertThat(ec2Result)
                                .containsEntry("status", "UP")
                                .containsEntry("instanceState", "running");
        }

        @Test
        @DisplayName("AWS 자격 증명 또는 권한 이슈로 SDK 예외 발생 시 DOWN 반환")
        void checkAwsStatus_Failure_WhenAwsException() {
                // given
                // S3Exception Mocking
                AwsErrorDetails s3ErrorDetails = AwsErrorDetails.builder()
                                .errorMessage("Access Denied")
                                .build();
                S3Exception s3Exception = (S3Exception) S3Exception.builder()
                                .awsErrorDetails(s3ErrorDetails)
                                .build();
                given(s3Client.headBucket(any(HeadBucketRequest.class))).willThrow(s3Exception);

                // Ec2Exception Mocking
                AwsErrorDetails ec2ErrorDetails = AwsErrorDetails.builder()
                                .errorMessage("The instance ID does not exist")
                                .build();
                Ec2Exception ec2Exception = (Ec2Exception) Ec2Exception.builder()
                                .awsErrorDetails(ec2ErrorDetails)
                                .build();
                given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willThrow(ec2Exception);

                // when
                Map<String, Object> result = awsHealthCheckService.checkAwsStatus();

                // then
                @SuppressWarnings("unchecked")
                Map<String, Object> s3Result = (Map<String, Object>) result.get("s3");
                assertThat(s3Result).containsEntry("status", "DOWN");
                assertThat(s3Result.get("message").toString()).contains("S3 Error: Access Denied");

                @SuppressWarnings("unchecked")
                Map<String, Object> ec2Result = (Map<String, Object>) result.get("ec2");
                assertThat(ec2Result).containsEntry("status", "DOWN");
                assertThat(ec2Result.get("message").toString()).contains("EC2 Error: The instance ID does not exist");
        }

        @Test
        @DisplayName("예기치 않은 일반 런타임 예외 발생 시 DOWN 반환")
        void checkAwsStatus_Failure_WhenUnexpectedException() {
                // given
                given(s3Client.headBucket(any(HeadBucketRequest.class)))
                                .willThrow(new RuntimeException("Connection Timeout"));
                given(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                                .willThrow(new RuntimeException("Internal Service Error"));

                // when
                Map<String, Object> result = awsHealthCheckService.checkAwsStatus();

                // then
                @SuppressWarnings("unchecked")
                Map<String, Object> s3Result = (Map<String, Object>) result.get("s3");
                assertThat(s3Result).containsEntry("status", "DOWN");
                assertThat(s3Result.get("message").toString()).contains("Unexpected Error: Connection Timeout");

                @SuppressWarnings("unchecked")
                Map<String, Object> ec2Result = (Map<String, Object>) result.get("ec2");
                assertThat(ec2Result).containsEntry("status", "DOWN");
                assertThat(ec2Result.get("message").toString()).contains("Unexpected Error: Internal Service Error");
        }
}