import { BtnProps } from "./types";
import * as S from "./styles";

/**
 * @description 범용 버튼 컴포넌트
 */
function Button({
  color,
  textColor,
  fontFamily,
  fontSize,
  borderRadius,
  children,
  ...rest
}: BtnProps) {
  return (
    <S.Btn color={color} borderRadius={borderRadius} {...rest} style={S.shadow}>
      <S.BtnText textColor={textColor} fontFamily={fontFamily} fontSize={fontSize}>
        {children}
      </S.BtnText>
    </S.Btn>
  );
}

export default Button;
