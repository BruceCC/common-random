package com.apifan.common.random.source;

import com.apifan.common.random.constant.CreditCardType;
import com.apifan.common.random.constant.Province;
import com.apifan.common.random.constant.RandomConstant;
import com.apifan.common.random.entity.IdPrefix;
import com.apifan.common.random.util.PinyinUtils;
import com.apifan.common.random.util.ResourceUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

/**
 * 个人信息数据源
 *
 * @author yin
 */
public class PersonInfoSource {
    private static final Logger logger = LoggerFactory.getLogger(PersonInfoSource.class);

    /**
     * 身份证加权因子
     */
    private static Map<Integer, Integer> weightingFactorMap = Maps.newHashMap();

    static {
        weightingFactorMap.put(1, 7);
        weightingFactorMap.put(2, 9);
        weightingFactorMap.put(3, 10);
        weightingFactorMap.put(4, 5);
        weightingFactorMap.put(5, 8);
        weightingFactorMap.put(6, 4);
        weightingFactorMap.put(7, 2);
        weightingFactorMap.put(8, 1);
        weightingFactorMap.put(9, 6);
        weightingFactorMap.put(10, 3);
    }

    /**
     * 身份证校验数
     */
    private static Map<Integer, String> checkNumMap = Maps.newHashMap();

    static {
        checkNumMap.put(0, "1");
        checkNumMap.put(1, "0");
        checkNumMap.put(2, "X");
        checkNumMap.put(3, "9");
        checkNumMap.put(4, "8");
        checkNumMap.put(5, "7");
        checkNumMap.put(6, "6");
        checkNumMap.put(7, "5");
        checkNumMap.put(8, "4");
        checkNumMap.put(9, "3");
        checkNumMap.put(10, "2");
    }

    /**
     * 常见中文姓氏
     */
    private List<String> lastNamesCN;

    /**
     * 常见中文女性名字
     */
    private List<String> femaleFirstNamesCN;

    /**
     * 常见中文男性名字
     */
    private List<String> maleFirstNamesCN;

    /**
     * 常见英文姓氏
     */
    private List<String> lastNamesEN;

    /**
     * 常见英文名字
     */
    private List<String> firstNamesEN;

    /**
     * 所有身份证前缀列表
     */
    private List<IdPrefix> idPrefixList = Lists.newArrayList();

    /**
     * 常见QQ非主流昵称
     */
    private List<String> qqNames;

    /**
     * 省级行政区身份证前缀映射
     */
    private Map<String, List<String>> provinceIdPrefixMap = Maps.newHashMap();

    private static final PersonInfoSource instance = new PersonInfoSource();

    private PersonInfoSource() {
        lastNamesCN = ResourceUtils.readLines("last-names-cn.txt");
        femaleFirstNamesCN = ResourceUtils.readLines("female-first-names-cn.txt");
        maleFirstNamesCN = ResourceUtils.readLines("male-first-names-cn.txt");
        lastNamesEN = ResourceUtils.readLines("last-names-en.txt");
        firstNamesEN = ResourceUtils.readLines("first-names-en.txt");
        qqNames = ResourceUtils.readLines("qq-name.txt");
        //解析身份证前缀数据
        List<String> lines = ResourceUtils.readLines("id-prefix.csv");
        if (CollectionUtils.isNotEmpty(lines)) {
            lines.forEach(i -> {
                if (StringUtils.isEmpty(i)) {
                    return;
                }
                List<String> row = Splitter.on(",").splitToList(i);
                IdPrefix prefix = new IdPrefix();
                prefix.setPrefix(row.get(0));
                prefix.setLocation(row.get(1));
                prefix.setParent(row.get(2));
                idPrefixList.add(prefix);
            });

            //找出所有省份
            Set<IdPrefix> provinceSet = new HashSet<>();
            idPrefixList.forEach(i -> {
                if (i == null) {
                    return;
                }
                if ("0".equals(i.getParent())) {
                    provinceSet.add(i);
                }
            });

            //建立映射关系
            provinceSet.forEach(p -> {
                if (p == null) {
                    return;
                }
                provinceIdPrefixMap.put(p.getLocation(), findIdPrefixByProvince(p));
            });
        }
    }

    /**
     * 获取唯一实例
     *
     * @return 实例
     */
    public static PersonInfoSource getInstance() {
        return instance;
    }

    /**
     * 生成随机的中文人名(性别随机)
     *
     * @return 随机中文人名
     */
    public String randomChineseName() {
        return randomChineseName(-1);
    }

    /**
     * 生成随机的男性中文人名
     *
     * @return 随机男性中文人名
     */
    public String randomMaleChineseName() {
        return randomChineseName(1);
    }

    /**
     * 生成随机的女性中文人名
     *
     * @return 随机女性中文人名
     */
    public String randomFemaleChineseName() {
        return randomChineseName(0);
    }

    /**
     * 生成随机的中文人名
     *
     * @param gender 性别标识：0女性，1男性，-1随机
     * @return 随机中文人名
     */
    private String randomChineseName(int gender) {
        //随机取一个常见姓氏
        Optional<String> lastName = Optional.ofNullable(ResourceUtils.getRandomElement(lastNamesCN));
        StringBuilder name = new StringBuilder(lastName.orElse(""));
        //名字1~2个字（随机）
        int length = RandomUtils.nextInt(1, 3);
        boolean isFemale;
        if (gender == 0) {
            isFemale = true;
        } else if (gender == 1) {
            isFemale = false;
        } else {
            isFemale = RandomUtils.nextInt(1, 99999) % 2 == 0;
        }
        for (int i = 0; i < length; i++) {
            if (isFemale) {
                name.append(ResourceUtils.getRandomElement(femaleFirstNamesCN));
            } else {
                name.append(ResourceUtils.getRandomElement(maleFirstNamesCN));
            }
        }
        return name.toString();
    }

    /**
     * 生成随机的英文人名
     *
     * @return 随机英文人名
     */
    public String randomEnglishName() {
        return ResourceUtils.getRandomElement(firstNamesEN) + " " + ResourceUtils.getRandomElement(lastNamesEN);
    }

    /**
     * 随机昵称(英文)
     *
     * @param maxLength 最大长度
     * @return 随机的昵称
     */
    public String randomNickName(int maxLength) {
        if (maxLength < 4) {
            maxLength = 4;
        }
        //必须以字母开头
        StringBuilder sb = new StringBuilder(RandomStringUtils.randomAlphabetic(1));
        int actualLength = RandomUtils.nextInt(4, maxLength + 1);
        sb.append(RandomStringUtils.randomAlphanumeric(actualLength - 1));
        return sb.toString();
    }

    /**
     * 随机昵称(中文)
     *
     * @param maxLength 最大长度
     * @return 随机的昵称
     */
    public String randomChineseNickName(int maxLength) {
        if (maxLength < 4) {
            maxLength = 4;
        }
        int x = RandomUtils.nextInt(1, 11);
        if (x % 2 == 0) {
            //女性名称常用字
            return ResourceUtils.getRandomString(femaleFirstNamesCN, RandomUtils.nextInt(2, maxLength + 1));
        } else {
            //男性名称常用字
            return ResourceUtils.getRandomString(maleFirstNamesCN, RandomUtils.nextInt(2, maxLength + 1));
        }
    }

    /**
     * 随机拼音昵称
     *
     * @param maxLength 最大长度
     * @return 随机拼音昵称
     */
    public String randomPinyinNickName(int maxLength) {
        String nickName = randomChineseNickName(maxLength);
        return PinyinUtils.toPinyin(nickName, true);
    }

    /**
     * 生成随机的中国手机号
     *
     * @return 随机中国手机号
     */
    public String randomChineseMobile() {
        Optional<String> prefix = Optional.ofNullable(ResourceUtils.getRandomElement(RandomConstant.mobilePrefixList));
        StringBuilder mobile = new StringBuilder(prefix.orElse(""));
        int x = mobile.length() == 3 ? 8 : 11 - mobile.length();
        for (int i = 0; i < x; i++) {
            mobile.append(RandomUtils.nextInt(0, 10));
        }
        return mobile.toString();
    }

    /**
     * 生成随机强密码
     *
     * @param length         长度
     * @param useSpecialChar 是否使用特殊字符
     * @return 随机强密码
     */
    public String randomStrongPassword(int length, boolean useSpecialChar) {
        if (length < 8) {
            //至少8位
            length = 8;
        }
        List<String> pwd = new ArrayList<>();
        //最多1/3的大写字母和小写字母
        int oneThirdCount = length / 3;
        for (int i = 0; i < oneThirdCount; i++) {
            pwd.add(RandomStringUtils.randomAlphabetic(1).toUpperCase());
            pwd.add(RandomStringUtils.randomAlphabetic(1).toLowerCase());
        }

        //特殊字符
        if (useSpecialChar) {
            int b = RandomUtils.nextInt(1, oneThirdCount);
            if (b > 0) {
                for (int i = 0; i < b; i++) {
                    pwd.add(ResourceUtils.getRandomElement(RandomConstant.specialCharList));
                }
            }
        }

        //剩下不足的用数字填充
        if (pwd.size() < length) {
            int digitCount = length - pwd.size();
            for (int i = 0; i < digitCount; i++) {
                pwd.add(RandomStringUtils.randomNumeric(1));
            }
        }

        //打乱顺序
        Collections.shuffle(pwd);
        return Joiner.on("").join(pwd);
    }

    /**
     * 生成随机的QQ号码
     *
     * @return 随机的QQ号码
     */
    public String randomQQAccount() {
        //目前QQ号码最短5位，最长11位
        return String.valueOf(RandomUtils.nextLong(10000L, 100000000000L));
    }

    /**
     * 生成随机的非主流QQ网名
     *
     * @return 随机的非主流QQ网名
     */
    public String randomQQNickName() {
        String name = ResourceUtils.getRandomString(qqNames, 1);
        return ResourceUtils.base64Decode(name);
    }

    /**
     * 生成随机男性身份证号码
     *
     * @param province  省级行政区枚举
     * @param beginDate 出生开始日期
     * @param endDate   出生结束日期
     * @return 随机男性身份证号码
     */
    public String randomMaleIdCard(Province province, LocalDate beginDate, LocalDate endDate) {
        Preconditions.checkNotNull(province);
        return randomIdCard(province.getName(), beginDate, endDate, 1);
    }

    /**
     * 生成随机男性身份证号码
     *
     * @param province 省级行政区枚举
     * @param age      年龄
     * @return 随机男性身份证号码
     */
    public String randomMaleIdCard(Province province, int age) {
        Preconditions.checkNotNull(province);
        return randomMaleIdCard(province.getName(), age);
    }

    /**
     * 生成随机男性身份证号码(按年龄段)
     *
     * @param province 省级行政区枚举
     * @param minAge   最小年龄(含)
     * @param maxAge   最大年龄(含)
     * @return 随机男性身份证号码
     */
    public String randomMaleIdCard(Province province, int minAge, int maxAge) {
        Preconditions.checkNotNull(province);
        return randomMaleIdCard(province.getName(), minAge, maxAge);
    }

    /**
     * 生成随机男性身份证号码
     *
     * @param provinceName 省级行政区名称(全称，留空则不限制)
     * @param beginDate    出生开始日期
     * @param endDate      出生结束日期
     * @return 随机男性身份证号码
     */
    public String randomMaleIdCard(String provinceName, LocalDate beginDate, LocalDate endDate) {
        return randomIdCard(provinceName, beginDate, endDate, 1);
    }

    /**
     * 生成随机男性身份证号码
     *
     * @param provinceName 省级行政区名称(全称，留空则不限制)
     * @param age          年龄
     * @return 随机男性身份证号码
     */
    public String randomMaleIdCard(String provinceName, int age) {
        LocalDate today = LocalDate.now();
        int year = today.getYear() - age;
        LocalDate beginDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return randomIdCard(provinceName, beginDate, endDate, 1);
    }

    /**
     * 生成随机男性身份证号码(按年龄段)
     *
     * @param provinceName 省级行政区名称(全称，留空则不限制)
     * @param minAge       最小年龄(含)
     * @param maxAge       最大年龄(含)
     * @return 随机男性身份证号码
     */
    public String randomMaleIdCard(String provinceName, int minAge, int maxAge) {
        Preconditions.checkArgument(minAge < maxAge, "年龄段错误");
        LocalDate today = LocalDate.now();
        int beginYear = today.getYear() - maxAge;
        int endYear = today.getYear() - minAge;
        LocalDate beginDate = LocalDate.of(beginYear, 1, 1);
        LocalDate endDate = LocalDate.of(endYear, 12, 31);
        return randomIdCard(provinceName, beginDate, endDate, 1);
    }

    /**
     * 生成随机女性身份证号码
     *
     * @param province  省级行政区枚举
     * @param beginDate 出生开始日期
     * @param endDate   出生结束日期
     * @return 随机女性身份证号码
     */
    public String randomFemaleIdCard(Province province, LocalDate beginDate, LocalDate endDate) {
        Preconditions.checkNotNull(province);
        return randomIdCard(province.getName(), beginDate, endDate, 0);
    }

    /**
     * 生成随机女性身份证号码(按年龄段)
     *
     * @param province 省级行政区枚举
     * @param minAge   最小年龄(含)
     * @param maxAge   最大年龄(含)
     * @return 随机女性身份证号码
     */
    public String randomFemaleIdCard(Province province, int minAge, int maxAge) {
        Preconditions.checkNotNull(province);
        return randomFemaleIdCard(province.getName(), minAge, maxAge);
    }

    /**
     * 生成随机女性身份证号码
     *
     * @param province 省级行政区枚举
     * @param age      年龄
     * @return 随机女性身份证号码
     */
    public String randomFemaleIdCard(Province province, int age) {
        Preconditions.checkNotNull(province);
        return randomFemaleIdCard(province.getName(), age);
    }

    /**
     * 生成随机女性身份证号码
     *
     * @param provinceName 省级行政区名称(全称，留空则不限制)
     * @param beginDate    出生开始日期
     * @param endDate      出生结束日期
     * @return 随机女性身份证号码
     */
    public String randomFemaleIdCard(String provinceName, LocalDate beginDate, LocalDate endDate) {
        return randomIdCard(provinceName, beginDate, endDate, 0);
    }

    /**
     * 生成随机女性身份证号码(按年龄段)
     *
     * @param provinceName 省级行政区名称(全称，留空则不限制)
     * @param minAge       最小年龄(含)
     * @param maxAge       最大年龄(含)
     * @return 随机女性身份证号码
     */
    public String randomFemaleIdCard(String provinceName, int minAge, int maxAge) {
        Preconditions.checkArgument(minAge < maxAge, "年龄段错误");
        LocalDate today = LocalDate.now();
        int beginYear = today.getYear() - maxAge;
        int endYear = today.getYear() - minAge;
        LocalDate beginDate = LocalDate.of(beginYear, 1, 1);
        LocalDate endDate = LocalDate.of(endYear, 12, 31);
        return randomIdCard(provinceName, beginDate, endDate, 0);
    }

    /**
     * 生成随机女性身份证号码
     *
     * @param provinceName 省级行政区名称(全称，留空则不限制)
     * @param age          年龄
     * @return 随机女性身份证号码
     */
    public String randomFemaleIdCard(String provinceName, int age) {
        LocalDate today = LocalDate.now();
        int year = today.getYear() - age;
        LocalDate beginDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return randomIdCard(provinceName, beginDate, endDate, 0);
    }

    /**
     * 随机信用卡号码
     * <p>注意: 此方法已移动到 {@link com.apifan.common.random.source.FinancialSource#randomCreditCardNo(CreditCardType) FinancialSource.randomCreditCardNo}</p>
     *
     * @param type 信用卡类型
     * @return 随机信用卡号码
     */
    @Deprecated
    public String randomCreditCardNo(CreditCardType type) {
        return FinancialSource.getInstance().randomCreditCardNo(type);
    }

    /**
     * 随机性别
     *
     * @return 性别标识：0女性，1男性
     */
    public int randomGender() {
        return RandomUtils.nextInt(0, 2);
    }

    /**
     * 生成随机身份证号码
     *
     * @param province  省级行政区名称(全称，留空则不限制)
     * @param beginDate 出生开始日期
     * @param endDate   出生结束日期
     * @param gender    性别标识：0女性，1男性
     * @return 随机身份证号码
     */
    private String randomIdCard(String province, LocalDate beginDate, LocalDate endDate, int gender) {
        Preconditions.checkArgument(beginDate != null, "开始日期为空");
        Preconditions.checkArgument(endDate != null, "结束日期为空");

        //随机获取前缀
        String prefix = "";
        if (StringUtils.isNotEmpty(province)) {
            List<String> prefixList = provinceIdPrefixMap.get(province);
            if (CollectionUtils.isNotEmpty(prefixList)) {
                prefix = ResourceUtils.getRandomElement(prefixList);
            }
        }
        if (StringUtils.isEmpty(prefix)) {
            //若为空，则从所有前缀中随机取一个
            Optional<IdPrefix> idPrefix = Optional.ofNullable(ResourceUtils.getRandomElement(idPrefixList));
            prefix = idPrefix.isPresent() ? idPrefix.get().getPrefix() : "";
        }

        //随机日期
        String date = DateTimeSource.getInstance().randomDate(beginDate, endDate, "yyyyMMdd");
        //随机3位顺序码
        int seq = RandomUtils.nextInt(1, 1000);
        if (gender == 0 && seq % 2 != 0) {
            //女性，但顺序码为奇数，则强转为偶数
            seq = seq - 1;
        } else if (gender == 1 && seq % 2 == 0) {
            //男性，但顺序码为偶数，则强转为奇数
            seq = seq + 1;
        }
        //前缀+日期+顺序码
        String src = prefix + date + String.format("%03d", seq);
        //校验和
        int sum = 0;
        for (int i = 1; i <= src.length(); i++) {
            int x = src.charAt(i - 1) - 48;
            int factor = (i <= 10 ? weightingFactorMap.get(i) : weightingFactorMap.get(i - 10));
            sum = sum + x * factor;
        }
        //校验和除以11取余数，再转换为1位校验数
        String checkNum = checkNumMap.get(sum % 11);
        return src + checkNum;
    }

    /**
     * 按省级行政区汇总身份证前缀
     *
     * @param provinceNode 省级行政区节点
     * @return 身份证前缀
     */
    private List<String> findIdPrefixByProvince(IdPrefix provinceNode) {
        if (provinceNode == null || StringUtils.isEmpty(provinceNode.getPrefix())) {
            return null;
        }
        List<String> resultList = Lists.newArrayList();
        idPrefixList.forEach(i -> {
            //前2位相同则表示属于同一个省级行政区
            String shortPrefix = provinceNode.getPrefix().substring(0, 2);
            if (i.getPrefix().startsWith(shortPrefix)) {
                resultList.add(i.getPrefix());
            }
        });
        return resultList;
    }
}
