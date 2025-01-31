import { QueryCache, QueryClient } from "@tanstack/react-query";
import { Alert } from "react-native";

export default new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 20 * 1000, // 20초동안 refetch 안함
      retry: 0,
    },
  },
  queryCache: new QueryCache({
    onError: (error) => {
      console.log(error.response?.data);
      if (error.response?.data.code !== "401") {
        Alert.alert(error.response?.data.message || "알 수 없는 에러가 발생했습니다.");
      }
    },
  }),
});

// Todo: 특정 query-key 별로 staleTime을 다르게 전역 설정시킬 수 있다.
// 이를 활용해 이전 달 캘린더라던가 일기 이런건 좀 길게 staleTime 가지고, 현재 달 캘린더와 일기 대화내역들은 짧게 (20초) staleTime 가지게 하는 것이 가능.

// queryClient.setQueryDefaults(todoKeys.all, { staleTime: 1000 * 60 }); // 60초동안 todoKeys 애들은 stale하도록 설정
