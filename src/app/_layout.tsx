import * as SplashScreen from "expo-splash-screen";
import * as Font from "expo-font";
import { Stack } from "expo-router";
import { ThemeProvider } from "styled-components/native";
import { theme } from "@/src/styles/theme";
import { useEffect, useState } from "react";

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const [isReady, setIsReady] = useState<boolean>(false);

  useEffect(() => {
    const prepare = async () => {
      try {
        await Font.loadAsync({
          nanumSquareNeoLight: require("@/assets/fonts/NanumSquareNeo-light.ttf"),
          nanumSquareNeoRegular: require("@/assets/fonts/NanumSquareNeo-regular.ttf"),
          nanumSquareNeoBold: require("@/assets/fonts/NanumSquareNeo-bold.ttf"),
          nanumSquareNeoExtraBold: require("@/assets/fonts/NanumSquareNeo-extrabold.ttf"),
          nanumSquareNeoHeavy: require("@/assets/fonts/NanumSquareNeo-heavy.ttf"),
        });
      } catch (e) {
        console.error(e);
      } finally {
        setIsReady(true);
        SplashScreen.hideAsync();
      }
    };

    prepare();
  }, []);

  if (!isReady) return null;

  return (
    <ThemeProvider theme={theme}>
      <Stack />
    </ThemeProvider>
  );
}
