package com.ckb.explore.worker.utils;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nervos.ckb.utils.AmountUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;

import org.nervos.ckb.utils.Numeric;
import org.springframework.util.StringUtils;

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
        private long expectedSupply;
        private long mintLimit;
        private int mintStatus;
    }

    public static OmigaInscriptionBasicInfo parseOmigaInscriptionInfo(String hexData) {
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
        long expectedSupply = hexToLongWithByteReverse(expectedSupplyHex);

        // 解析mint_limit (需要字节反转)
        String mintLimitHex = data.substring(0, 32);
        data = data.substring(32);
        long mintLimit = hexToLongWithByteReverse(mintLimitHex);

        // 解析mint_status
        int mintStatus = Integer.parseInt(data.substring(0, 2), 16);
        data = data.substring(2);

        return new OmigaInscriptionBasicInfo(decimal,name,symbol,udtHash,expectedSupply,mintLimit,mintStatus);
    }

    private static long hexToLongWithByteReverse(String hex) {
        // 确保十六进制字符串长度为32
        if (hex == null || hex.length() != 32) {
            return 0;
        }
        // 反转字节顺序
        StringBuilder reversedHex = new StringBuilder();
        for (int i = hex.length() - 2; i >= 0; i -= 2) {
            reversedHex.append(hex.substring(i, i + 2));
        }
        // 转换为长整数
        return Long.parseLong(reversedHex.toString(), 16);
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
            e.printStackTrace();
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

    public  static  void  main(String[] args){
      StringBuffer outputData = new StringBuffer();
      outputData.append("0x650a0000100000001d000000650a000009000000696d6167652f706e67440a000089504e470d0a1a0a0000000d4948445200000064000000640803000000473c656600000237504c5445c7cdebfffcf4c7cdeb000000020100f8eac7ffc43bb7690ff6a02ec6cceaffffffadb1c6020202f49c22acb0c5b6690f807d75fdfaf2b0b5cc9ba5d9f8e9c67c4100b1b6cdb76b0fc5cbe8fec33b84220ffffefef69f2eacb0c4ef8624c1770fe78923ca9419da8413f0ddb1db8513b4b9d4f49e2dffc43c360902fefdfcf8e8c5f4cb94fdc23bee85249aa4d8f2dfb2fadfa0b3650d6e2e017b4100f5e7c4f3e5c3f9e0bcf9e4b375726bfdcf64fd8959f6bc45fbc039f9af2eb1610d723800f9f5f0f6f2ecc1c7e6acb5e2a9b0ccf8dbb6fadb91fbd988fcd57bfd9566fdc44ff8aa2a141413ad5d0da7530da34f0c1602010b0300faf6f2e9e9e9b7bee4bbc0ddf1e3c0f8e8bff0d9ba999db0f9d5aff8e2adf9d2acf9e2a8f9cea7facaa3ffe19bfadd99f1c585fdd883fbad83fbd2727d7a72fb9d707a7870fcd16c6c6962fd7844ecb636f29128f08c26f6a225dda524e88a23d97a20e7921de2891d82210ead5a0c893c086030074c1a05662903210601783d00fffcf6efece5ebe8e1a4aedca4addcfff2d4b3b8d3949ed0fce8cce5d0cb8f99c8ffecc0dec5bfbcbcbc9fa3b7ffe8b37e86b1facba4f5d8a3eccc9dedcb99fabd94c29389fbb287fba77af2c676f4ba6cad6c5f63615bfccb59f27e4ffd804efd7f4dffc744f8b03ffe6f3bfcba35e7b235f6a635964433fbb632d9a732f5a130e9972cb88c2bdc8e29c78225222222db8221ef9820cc731ec17b1a892c1a040418312814b76c10923710b8710e2a210da9580c96460a6e2306551903431403dee61b780000000174524e53fe1ae3077d000007a14944415468deecd6cf6b13411407f0d6ef38531696a6600e2592984b63a91e2a142ab4a0e7aa5804cfa2504aa9582c58f1d7c19307efe24550503c88088228f8c7f9de9b797d4e68bbd934851ef2856e9609799f7933dd4c26ce9c7c26c6c86945264f2c63648c4c6a4e03829493400cb8e43990b8512352df52705ae48c10016080229c79b81121c0acd5eec94d4f9516dc4810209bbf97bcdd57ba70c74680428d0f4501e0b5ed3cdc9d96f73b70c744805e1456c0590c78c34248994ed8b1100800ca5e087b5cfd265fb00c4da2dcf00822100034b4e8b3108046c38638d370c3223a5334a4ace4c91780011b72decff2cb7008424a366fef9f2e32b04597f7a0ac3b4fe9c2d542ccb8821856b69292f6c86030224a7d0494955471d9564790200a8fcb8698520fb147107df3d6b1cdc6060d6d5a2b3ba654226628a2ca06fd85884c67f0df4fb195565da4287244166c99915cb1f57a5014a254236624054115506e29f23b3023f91304e9e9d7e5a0c80d41ee29620fb7aee22cb2ef1627062b351032b495db5a4b0f46cb36244c30624a3562c64bef2323f19ad505c9fdb22c574d1c16f17df9fca8a4bc5b5880c4959c357df76a44bed5435afb532f358899e29c176897463fe6ad0c86c0cbb61bf0035a5d02319c4ad2112bf36c740746527879b4b804fdc85767102f5cb75b13112195855cf23ca7f22259476b359192a235a270f76c9e7390f75511a6d3a981ac88316506fa04759c0089bb8c4ea753859862ff4b17d5384481f5d3ae8744439b801a87f4a28620181881229239322a957634dcb5aa87315780871121e348059abc8d1c4186007479855d56daedb98a46284b4eba008e3e7e01984149afdf81c7f4f9dc409cbefbaf152702dccc11082b5a7a867f3b2452faa71c8c2c191209fa6cb3e2f88507a027bb36769d9d5f09110c58d2ca8e2eeb2e228ea787661532896d2a1e05b0a201ac094a367f17ef98f88926801755673c1221b79952b1f1d4546ce3029a15882880563605ffdab5ef1627a2280ae030f724d9318f0c934427c655c2ae75117b41057bc7debbd810110445c5f2878a0d157b412c2828888a88a208fae53cefce8ccfa0718c822878d8dd6c3293f79bfbee1b92d94d674706334ed09b11de4cfbf06384417a20eef710833bcef642d51e714f8fdee8d1c73530b9ea382100fe60c622cc409a13866ad813b2df42c9f54cd8b4f5caa6093d3959c3a1d90d3eb261cbd62d73249d2aa58ea02d4423033a6c16a267c3ce0593272fb835a7476c0c910937f888a010c898ded30431c5527e1179841cb0fbe1debd0f768be6f16d227376de1704c154de9fd61b0789d8f525a67de4c0be8e8e279c9cb513219a828da0ca565842e252c6da491b3b06008d369150e4f5be676a6cde060269022b288160b1089569c71cd00e424387e1ba9ab879d4dc1d18fd9582a5da9b49bb209c310227d88cf61067889edc1b47cd9d377f8f38844ac0ef3df3e76da300ee0c8461db088dd13450cd09336adefc5d9310d8a48a963977d446e4ba4d1862601778d31e42e32820e249d5164067d2bd8502264811143e61d444982a7c08060eec1c0cb3c239d948c89e2fc6208b2c9b1a45ef62078dfd8d862a5a4940d28ccf75c3f3690c065e7402f869848404f02c02294cadcfa8471c160d3924d2900481c9d98cec365e8db365560a5e76746621ae0c40004f114fa2fa943bf9a55141f64b9d9104c1b82f862fe8423f5bea4f23a1707cf89ee72748f46658be4ea4d18ca40677b4850c03880cce429ca1f111238568d8f67537a34804340ec121d591dddd76e79ab6a4df3016225c625988337c5f92e90aa2a83e242e054cda1343a11b405cc85181806f6d8c0f84594808890d0812646814ade835e5eefb029546e3556240857c5fe3b3902e598461343a2dc9627e88d0f044cb40dfb4f1185a88eafdae4fb9905c9d12e097408ee76d86c3162222b8c842b4413e9596881a16f105796b29e2c947ced7bafc94a78524121c861981be36c3616a8a006021358b78545a233414a1d11729c252d89573d77a5d8ddc857ba55219011b53a99c4157d750a36d87a7882aad106d87f89ee4b5104518b1caf47ef5283504232a2ea74e824cdc765d6a2a6520e223414ec708acb2a41e45897119cfe1941af76055cbbb0e76428d9f4220f918f18560a28c0ebe1898354b8055ab667a5fa5729ee5181aa6fc3308ca6f130444a0db81a1439706364bf440cd70c0b769724cc55f6dca62321126aec4b77b02abe3e757000803543cc6ae70cdecaf15bf06d3a76cbc0cc48f77b0089a8e7356451daeaa1831fae77b3d15532126fad3606a3f40e027465fe90388739aa3a56886aba2024cb1d41fa9d11ae1c83e0d3e1952ee43c6cdfc37481c3687db675a8184985a6c98526b04e5b20805c0e37497e9104aa5262ebd5f1388a8c02a6092d698627fd30261b4b5120f05d024a4529a746ca431b301272801355a22218562918a32ea28a492261d3abdcf6d255861e657841a2d115e85a3542a02703303955a2526dd0203d6b33726eb450b522a169de3a46fe386770b8c6588c97ef9052e959c4329333ea382edff7ad0c84294011975546a69b9554081b1fdfff94b8719965148a5d47271a3ebf85a030913b6f73615705062b9b8d11df06b970e0e4a2d17377a13d03ee2206735a7ff22c001994866f0bd987ff7fff1ff11efb7f3577d96a829ff2af227f2197b1f0f48907ccc680000000e655849664d4d002a0000000800000000000000d253930000000049454e44ae426082");
      System.out.println(parseSporeCellData(outputData.toString()));
    }
}
