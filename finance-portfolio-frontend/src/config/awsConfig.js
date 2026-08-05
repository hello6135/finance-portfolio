import { fromCognitoIdentityPool } from "@aws-sdk/credential-provider-cognito-identity";

const region = import.meta.env.VITE_AWS_REGION || "ap-northeast-2";
const identityPoolId = import.meta.env.VITE_AWS_COGNITO_IDENTITY_POOL_ID;

if (!identityPoolId) {
  throw new Error("VITE_AWS_COGNITO_IDENTITY_POOL_ID 환경변수가 설정되지 않았습니다.");
}

export const awsRegion = region;

export const cognitoCredentials = fromCognitoIdentityPool({
  clientConfig: { region },
  identityPoolId,
});