package com.ckb.explore.worker.utils;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nervos.ckb.utils.AmountUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;

import org.nervos.ckb.utils.Numeric;
import org.springframework.util.StringUtils;

@Slf4j
public class CkbUtil {

    private static final Pattern ASCII_PATTERN = Pattern.compile("^[\\x21-\\x7E]+(?:\\s[\\x21-\\x7E]+)?$");

    private static final Pattern CONTROL_CHARS_PATTERN = Pattern.compile("[\\x00-\\x1F\\x7F\u2028\u2029\u200B]");

    private static final ObjectMapper objectMapper = new ObjectMapper();


    public static TokenClassData parseTokenClassData(String data) {
        try {
            // 去除前缀"0x"
            if (data.startsWith("0x")) {
                data = data.substring(2);
            }

            // 解析版本号 (0-1字符)
            int version = Integer.parseInt(data.substring(0, 2), 16);

            // 解析总数 (2-9字符)
            long total = Long.parseLong(data.substring(2, 10), 16);

            // 解析已发行数量 (10-17字符)
            long issued = Long.parseLong(data.substring(10, 18), 16);

            // 解析配置 (18-19字符)
            int configure = Integer.parseInt(data.substring(18, 20), 16);

            // 解析名称大小 (20-23字符)
            int nameSize = Integer.parseInt(data.substring(20, 24), 16);

            // 计算名称的结束索引并解析名称
            int nameEndIndex = 24 + (nameSize * 2) - 1;
            // 确保索引不超过字符串长度
            nameEndIndex = Math.min(nameEndIndex, data.length() - 1);
            String nameHex = data.substring(24, nameEndIndex + 1);
            String name = hexToString(nameHex);

            // 解析描述大小
            int descriptionSizeStartIndex = nameEndIndex + 1;
            int descriptionSizeEndIndex = descriptionSizeStartIndex + 4 - 1;
            // 检查是否有足够的字符可供解析
            if (descriptionSizeEndIndex >= data.length()) {
                return getDefaultTokenClassData();
            }
            int descriptionSize = Integer.parseInt(data.substring(descriptionSizeStartIndex, descriptionSizeEndIndex + 1), 16);

            // 解析描述
            int descriptionStartIndex = descriptionSizeEndIndex + 1;
            int descriptionEndIndex = descriptionStartIndex + (descriptionSize * 2) - 1;
            descriptionEndIndex = Math.min(descriptionEndIndex, data.length() - 1);
            String descriptionHex = data.substring(descriptionStartIndex, descriptionEndIndex + 1);
            String description = hexToString(descriptionHex);

            // 解析渲染器大小
            int rendererSizeStartIndex = descriptionEndIndex + 1;
            int rendererSizeEndIndex = rendererSizeStartIndex + 4 - 1;
            if (rendererSizeEndIndex >= data.length()) {
                return getDefaultTokenClassData();
            }
            int rendererSize = Integer.parseInt(data.substring(rendererSizeStartIndex, rendererSizeEndIndex + 1), 16);

            // 解析渲染器
            int rendererStartIndex = rendererSizeEndIndex + 1;
            int rendererEndIndex = rendererStartIndex + (rendererSize * 2) - 1;
            rendererEndIndex = Math.min(rendererEndIndex, data.length() - 1);
            String rendererHex = data.substring(rendererStartIndex, rendererEndIndex + 1);
            String renderer = hexToString(rendererHex);

            // 返回解析结果
            return new TokenClassData(version, total, issued, configure, name, description, renderer);

        } catch (Exception e) {
            // 发生任何异常时返回默认值
            log.error("parseTokenClassData error",e);
            return getDefaultTokenClassData();
        }
    }

    /**
     * 将十六进制字符串转换为UTF-8编码的字符串，并处理无效字符
     * @param hex 十六进制字符串
     * @return 转换后的字符串
     */
    private static String hexToString(String hex) {
        if (hex == null || hex.isEmpty()) {
            return "";
        }

        // 确保十六进制字符串长度为偶数
        if (hex.length() % 2 != 0) {
            hex += "0";
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
        }

        // 转换为UTF-8字符串，替换无效和未定义的字符
        String str = new String(bytes, StandardCharsets.UTF_8);
        // 删除空字符并返回
        return str.replace("\u0000", "");
    }

    private static TokenClassData getDefaultTokenClassData() {
        return new TokenClassData(0, 0, 0, 0, "", "", "");
    }

    /**
     * 令牌类数据模型类，对应Ruby中的OpenStruct
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenClassData {
        private  int version;
        private  long total;
        private  long issued;
        private  int configure;
        private  String name;
        private  String description;
        private  String renderer;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UdtBasicInfo {
        private  int decimal;
        private  String name;
        private  String symbol;
    }

    public static UdtBasicInfo parseUniqueCell(String hexData) {

        String data = hexData.startsWith("0x") ? hexData.substring(2) : hexData;

        // 解析decimal (前2个字符)
        String decimalHex = data.substring(0, 2);
        int decimal = Integer.parseInt(decimalHex, 16);
        data = data.substring(2);

        // 解析name长度和name
        String nameLenHex = data.substring(0, 2);
        int nameLen = Integer.parseInt(nameLenHex, 16);
        data = data.substring(2);

        String nameHex = data.substring(0, nameLen * 2);
        String name = hexToString(nameHex);
        data = data.substring(nameLen * 2);
        // 移除空字符
        String cleanedName = name.replaceAll("\u0000", "");

        // 解析symbol长度和symbol
        String symbolLenHex = data.substring(0, 2);
        int symbolLen = Integer.parseInt(symbolLenHex, 16);
        data = data.substring(2);

        String symbolHex = data.substring(0, symbolLen * 2);
        String symbol = hexToString(symbolHex);
        // 移除空字符
        String cleanedSymbol = symbol.replaceAll("\u0000", "");

         UdtBasicInfo udtBasicInfo = new UdtBasicInfo(decimal, cleanedName, cleanedSymbol);
         return  udtBasicInfo;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SporeCellData {
        private  String contentType;
        private  String content;
        private  String clusterId;

    }

    public static SporeCellData parseSporeCellData(String hexData) {
        try {
            String data = hexData.substring(2);

            // 解析各偏移量（8个十六进制字符 = 4字节，转换为long型后乘以2）
            long contentTypeOffset = parseOffset(data, 8, 8) * 2;
            long contentOffset = parseOffset(data, 16, 8) * 2;
            long clusterIdOffset = parseOffset(data, 24, 8) * 2;

            // 解析content_type
            String contentTypeHex = data.substring(
                    (int) (contentTypeOffset + 8),
                    (int) contentOffset
            );
            String contentType = hexToString(contentTypeHex);

            // 解析content
            String content = data.substring(
                    (int) (contentOffset + 8),
                    (int) clusterIdOffset
            );

            // 解析cluster_id
            String clusterId = null;
            if (clusterIdOffset + 8 <= data.length()) {
                String clusterIdHex = data.substring((int) (clusterIdOffset + 8));
                clusterId = "0x" + clusterIdHex;
            }

            return new SporeCellData(contentType,content,clusterId);

        } catch (Exception e) {
            log.error("parseSporeCellData error",e);
            // 发生任何异常时返回空值
            return new SporeCellData();
        }

    }

    private static long parseOffset(String data, int start, int length) {
        String hex = data.substring(start, start + length);
        byte[] bytes = HexFormat.of().parseHex(hex);
        // 转换为小端模式的long值（对应Ruby的unpack1("l")）
        return java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OmigaInscriptionBasicInfo {
        private  int decimal;
        private  String name;
        private  String symbol;
        private String udtHash;
        private BigInteger expectedSupply;
        private BigInteger mintLimit;
        private int mintStatus;
    }

    public static OmigaInscriptionBasicInfo parseOmigaInscriptionInfo(String hexData) {
        try {
            // 移除前缀"0x"
            String data = hexData.startsWith("0x") ? hexData.substring(2) : hexData;

            // 解析decimal值
            int decimal = Integer.parseInt(data.substring(0, 2), 16);
            data = data.substring(2);

            // 解析名称
            int nameLen = Integer.parseInt(data.substring(0, 2), 16);
            data = data.substring(2);
            String name = hexToString(data.substring(0, nameLen * 2));
            data = data.substring(nameLen * 2);

            // 解析符号
            int symbolLen = Integer.parseInt(data.substring(0, 2), 16);
            data = data.substring(2);
            String symbol = hexToString(data.substring(0, symbolLen * 2));
            data = data.substring(symbolLen * 2);

            // 解析udt_hash
            String udtHash = "0x" + data.substring(0, 64);
            data = data.substring(64);

            // 解析expected_supply (需要字节反转)
            String expectedSupplyHex = data.substring(0, 32);
            data = data.substring(32);
//            long expectedSupply = hexToLongWithByteReverse(expectedSupplyHex);
            BigInteger expectedSupply = hexToBigIntegerWithByteReverse(expectedSupplyHex);
            // 解析mint_limit (需要字节反转)
            String mintLimitHex = data.substring(0, 32);
            data = data.substring(32);
//            long mintLimit = hexToLongWithByteReverse(mintLimitHex);
             BigInteger mintLimit = hexToBigIntegerWithByteReverse(mintLimitHex);
            // 解析mint_status
            int mintStatus = Integer.parseInt(data.substring(0, 2), 16);
            data = data.substring(2);

            return new OmigaInscriptionBasicInfo(decimal, name, symbol, udtHash, expectedSupply, mintLimit, mintStatus);
        }catch (Exception e){
            log.error("OmigaInscriptionBasicInfo error",e);

            return new OmigaInscriptionBasicInfo();
        }
    }

    private static BigInteger hexToBigIntegerWithByteReverse(String hex) {
        // 确保十六进制字符串长度为32
        if (hex == null || hex.length() != 32) {
            return BigInteger.ZERO;
        }
        // 反转字节顺序
        StringBuilder reversedHex = new StringBuilder();
        for (int i = hex.length() - 2; i >= 0; i -= 2) {
            reversedHex.append(hex.substring(i, i + 2));
        }
        // 转换为长整数
        return  new BigInteger(reversedHex.toString(),16);

    }


    public static boolean isAsciiOnly(String str) {
        if (str == null) {
            return false; // 或根据需求处理 null（Ruby 中 nil 调用方法会报错）
        }
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            // ASCII 字符的 Unicode 编码范围是 0-127
            if (c > 127) {
                return false;
            }
        }
        return true;
    }


    public static BigInteger dataToUdtAmount(byte[] data){
        //u128
        if(data!=null&data.length>=16){
            byte[] udtAmountData = new byte[16];
            System.arraycopy(data, 0, udtAmountData, 0, udtAmountData.length);
            return AmountUtils.dataToSudtAmount(udtAmountData);
        }else {
            return BigInteger.ZERO;
        }

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SporeClusterData {
        private  String name;
        private  String description;

    }

    public static SporeClusterData parseSporeClusterData(String hexData) {
        try {
            // 截取从索引2开始到结尾的子字符串
            String data = hexData.substring(2);

            // 解析名称偏移量
            long nameOffset =parseOffset(data, 8, 8) * 2;

            // 解析描述偏移量
            long descriptionOffset = parseOffset(data, 16, 8) * 2;
            // 提取名称

            String nameHex = data.substring(
                    (int) (nameOffset + 8),
                    (int) descriptionOffset
            );
            String name = hexToString(nameHex);

            // 提取描述 是否需要提取还  提取测试一个 有的挺长 而且没用做展示
            String descHex = data.substring(
                    (int) (descriptionOffset + 8),
                    data.length()
            );
            String description = hexToString(descHex);


            // 如果名称过长，截断并添加省略号
            if (name.length() > 100) {
                name = name.substring(0, 97) + "...";
            }
            name = sanitizeString(name);
            description = sanitizeString(description);
            try {
                Map <String,Object> map = objectMapper.readValue(description, Map.class);
                if(map.get("description")!=null){
                    description = map.get("description").toString();
                }
            }catch (Exception e){

            }

            return new SporeClusterData(name,description);

        } catch (Exception e) {
            // 发生异常时返回
            log.error("parseSporeClusterData error",e);
            return new SporeClusterData();
        }
    }

    private static int hexToInt(String hex) {
        // 使用大端字节序解析32位有符号整数
        return Integer.parseInt(hex, 16);
    }



    private static String sanitizeString(String str) {
        if (str == null) {
            return "";
        }

        try {
              byte[] utf8Bytes = str.getBytes(StandardCharsets.UTF_8);
            String utf8Str = new String(utf8Bytes, StandardCharsets.UTF_8);

            // 2. 移除空白字符
            String replaced = utf8Str.replace("\uFFFD", "");

            // 3. 移除控制字符及特定Unicode空白字符
            return CONTROL_CHARS_PATTERN.matcher(replaced).replaceAll("");
        } catch (Exception e) {
            // 捕获编码转换相关异常（对应Ruby的rescue）
            return "";
        }
    }




  /**
   * DAO数据模型类，对应Ruby中的OpenStruct
   */
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class DaoDto {
    private BigInteger cI;
    private BigInteger arI;
    private BigInteger sI;
    private BigInteger uI;
  }

  /**
   * 解析DAO数据
   * @param dao DAO字节数组
   * @return DaoDto对象包含解析后的DAO数据
   */
  public static DaoDto parseDao(byte[] dao) {
    if (dao == null || dao.length < 32) {
      return null;
    }

    try {
      // 解析cI (0-7字节)
      byte[] cIBytes = new byte[8];
      System.arraycopy(dao, 0, cIBytes, 0, 8);
      BigInteger cI = BigInteger.valueOf(littleEndianBytesToLong(cIBytes));

      // 解析arI (8-15字节)
      byte[] arIBytes = new byte[8];
      System.arraycopy(dao, 8, arIBytes, 0, 8);
      BigInteger arI = BigInteger.valueOf(littleEndianBytesToLong(arIBytes));

      // 解析sI (16-23字节)
      byte[] sIBytes = new byte[8];
      System.arraycopy(dao, 16, sIBytes, 0, 8);
      BigInteger sI = BigInteger.valueOf(littleEndianBytesToLong(sIBytes));

      // 解析uI (24-31字节)
      byte[] uIBytes = new byte[8];
      System.arraycopy(dao, 24, uIBytes, 0, 8);
      BigInteger uI = BigInteger.valueOf(littleEndianBytesToLong(uIBytes));

      return new DaoDto(cI, arI, sI, uI);
    } catch (Exception e) {
      // 发生任何异常时返回null
      return null;
    }
  }

  /**
   * 将小端序字节数组转换为long值
   * 对应Ruby中的unpack("Q<")操作
   * @param bytes 小端序字节数组
   * @return 转换后的long值
   */
  private static long littleEndianBytesToLong(byte[] bytes) {
    // 严格校验：必须为8字节（64位），否则属于非法输入
    if (bytes == null) {
      throw new IllegalArgumentException("字节数组不能为null（需8字节小端序数组）");
    }
    if (bytes.length != 8) {
      throw new IllegalArgumentException("字节数组长度必须为8（实际长度：" + bytes.length + "）");
    }
    return ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong();
  }

  /**
   * 将字节数组转换为块号
   * @param data 字节数组
   * @return 块号
   */
  public static Long convertToBlockNumber(byte[] data) {

    // 获取data并处理空值
    if (data == null || data.length == 0) {
      throw new IllegalArgumentException("data cannot be null or empty");
    }

    // 小端转换
    Long bigIntValue = littleEndianBytesToLong(data);

    return bigIntValue;
  }



    public static boolean invalidChar(String name){
        return StringUtils.hasLength(name)&&CkbUtil.isAsciiOnly(name);
    }

    public static boolean invisibleChar(String name){
        return  StringUtils.hasLength(name)&&!ASCII_PATTERN.matcher(name).matches();
    }

    public static boolean outOfLength(String name){
        return StringUtils.hasLength(name)&&name.length()>60;
    }

}
