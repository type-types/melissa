import { Alert } from "react-native";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "expo-router";
import { isAxiosError } from "axios";
import { googleLoginFn, kakaoLoginFn } from "../apis/loginApi";
import { setSecureValue } from "../libs/secureStorage";
import { setStorageValue } from "../libs/mmkv";
import { ErrorResponse } from "../types/commonTypes";
import { LoginType } from "../types/loginTypes";

const useLogin = () => {
  const queryClient = useQueryClient();
  const router = useRouter();

  const handleSuccess = async (data: LoginType) => {
    console.log(`${data.result.oauthProvider} 로그인 성공!`);
    setStorageValue("accessToken", `${data.result.tokenType} ${data.result.accessToken}`);
    await setSecureValue("refreshToken", data.result.refreshToken);
    queryClient.invalidateQueries({ queryKey: ["check-new-user"] });
    router.replace("/(app)");
  };

  const handleError = (error: unknown) => {
    console.error("로그인 실패!", error);
    if (isAxiosError<ErrorResponse>(error)) {
      console.error("OAuth 프로바이더 정상 작동, 백엔드와 문제 발생", error.response?.data);
      Alert.alert("로그인에 실패했습니다.");
    }
  };

  const { isPending: kakaoIsPending, mutate: kakaoMutate } = useMutation({
    mutationFn: kakaoLoginFn,
    onSuccess: handleSuccess,
    onError: handleError,
  });

  const { isPending: googleIsPending, mutate: googleMutate } = useMutation({
    mutationFn: googleLoginFn,
    onSuccess: handleSuccess,
    onError: handleError,
  });

  const isPending = kakaoIsPending || googleIsPending;

  return { isPending, kakaoMutate, googleMutate };
};

export default useLogin;
