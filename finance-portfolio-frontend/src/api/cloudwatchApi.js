import { CloudWatchLogsClient, FilterLogEventsCommand } from "@aws-sdk/client-cloudwatch-logs";
import { cognitoCredentials, awsRegion } from "../config/awsConfig";


/**
 * AWS CloudWatch Logs 서비스로부터 실시간 도커 컨테이너 로그를 격리 쿼리 및 수집하는 API 함수
 * @param {Object} queryParams - 필터링 및 검색 매개변수
 * @param {string} queryParams.level - 로그 레벨 스코프 (ALL, INFO, WARN, ERROR)
 * @param {string} queryParams.search - 정규식 및 일반 텍스트 검색 키워드
 * @returns {Promise<Array>} 포맷팅이 완료된 시스템 로그 객체 배열
 */
export const getCloudWatchLogs = async ({ level, search }) => {

    const targetStreamName = import.meta.env.VITE_AWS_LOG_STREAM_NAME;

    // AWS SDK 가이드라인에 따른 명시적 클라이언트 인스턴스화
    const cwlClient = new CloudWatchLogsClient({
        region: awsRegion,
        credentials: cognitoCredentials
    });

    // 검색식 스코프 빌드
    let filterPattern = "";
    if (level && level !== "ALL") {
        filterPattern = `"${level}"`;
    }
    if (search) {
        filterPattern += ` "${search}"`;
    }

    const command = new FilterLogEventsCommand({
        logGroupName: "finance-portfolio-backend-log",
        logStreamNames: targetStreamName ? [targetStreamName] : undefined,
        filterPattern: filterPattern || undefined,
        limit: 50
    });

    const response = await cwlClient.send(command);

    // UI 데이터 정규화 및 추상화 매핑
    return (response.events || []).map((event, index) => {
        const rawMessage = event.message || "";
        let detectedLevel = "INFO";
        if (rawMessage.includes("ERROR")) detectedLevel = "ERROR";
        else if (rawMessage.includes("WARN")) detectedLevel = "WARN";

        // eventId가 없을 경우 timestamp + index로 고유 key 보장
        const uniqueId = event.eventId || `${event.timestamp}-${index}`;

        return {
            id: uniqueId,
            timestamp: new Date(event.timestamp).toLocaleString('ko-KR'),
            message: rawMessage,
            level: detectedLevel
        };
    });
};