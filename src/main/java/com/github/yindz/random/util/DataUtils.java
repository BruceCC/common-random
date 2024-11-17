package com.github.yindz.random.util;

import com.github.yindz.random.entity.DataField;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据生成工具
 * <p>用于批量快速生成随机虚拟数据</p>
 *
 * @author yin
 * @since 1.0.10
 */
public class DataUtils {
    private static final Logger logger = LoggerFactory.getLogger(DataUtils.class);

    /**
     * 生成JSON
     *
     * @param fieldList 数据字段定义
     * @param total     数量
     * @return JSON字符串
     */
    public static String generateJson(List<DataField> fieldList, int total) {
        List<String> strList = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            strList.add(generateJson(fieldList));
        }
        return "[" + Joiner.on(",").join(strList) + "]";
    }

    /**
     * 生成SQL
     *
     * @param fieldList 数据字段定义
     * @param tableName 表名
     * @param total     数量
     * @return SQL
     */
    public static String generateSql(List<DataField> fieldList, String tableName, int total) {
        fieldList = skipInvalidFields(fieldList);
        List<String> columnsList = new ArrayList<>();
        fieldList.forEach(f -> columnsList.add(f.getField()));
        List<String> dataList = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            List<String> paramsList = new ArrayList<>();
            fieldList.forEach(f -> {
                Object value = f.getValueSupplier().get();
                String valueStr = String.valueOf(value);
                boolean wrap = value instanceof String;
                paramsList.add(wrap ? StringUtils.wrap(valueStr, "\"") : valueStr);
            });
            dataList.add("select " + Joiner.on(",").join(paramsList) + " from dual");
        }
        return "insert into " + tableName + "(" + Joiner.on(",").join(columnsList) + ")\n" + Joiner.on("\nunion all\n").join(dataList);
    }

    /**
     * 生成CSV
     *
     * @param fieldList 数据字段定义
     * @param total     数量
     * @return CSV
     */
    public static String generateCsv(List<DataField> fieldList, int total) {
        fieldList = skipInvalidFields(fieldList);
        List<String> columnsList = new ArrayList<>();
        fieldList.forEach(f -> columnsList.add(StringUtils.wrap(f.getField(), "\"")));
        List<String> dataList = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            List<String> paramsList = new ArrayList<>();
            fieldList.forEach(f -> paramsList.add(StringUtils.wrap(String.valueOf(f.getValueSupplier().get()), "\"")));
            dataList.add(Joiner.on(",").join(paramsList));
        }
        return Joiner.on(",").join(columnsList) + "\n" + Joiner.on("\n").join(dataList);
    }

    /**
     * 生成对象
     *
     * @param fieldList 数据字段定义
     * @param clazz     对象类型
     * @param <T>       泛型
     * @return
     * @throws Exception
     */
    public static <T> T generateObject(List<DataField> fieldList, Class<T> clazz) throws Exception {
        Preconditions.checkArgument(clazz != null, "对象类型为空");
        return JsonUtils.parseObject(generateJson(fieldList), clazz);
    }

    /**
     * 生成对象列表
     *
     * @param fieldList 数据字段定义
     * @param clazz     对象类型
     * @param total     条数
     * @param <T>       泛型
     * @return
     * @throws Exception
     */
    public static <T> List<T> generateObjectList(List<DataField> fieldList, Class<T> clazz, int total) throws Exception {
        Preconditions.checkArgument(clazz != null, "对象类型为空");
        Preconditions.checkArgument(total > 0, "数量必须大于0");
        List<T> resultList = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            resultList.add(generateObject(fieldList, clazz));
        }
        return resultList;
    }

    /**
     * 过滤无效的数据字段定义
     *
     * @param fieldList 数据字段定义
     * @return 保留有效的数据字段定义
     */
    private static List<DataField> skipInvalidFields(List<DataField> fieldList) {
        if (CollectionUtils.isEmpty(fieldList)) {
            throw new RuntimeException("数据字段定义为空!");
        }
        List<DataField> validList = fieldList.stream().filter(Objects::nonNull).filter(i -> StringUtils.isNotBlank(i.getField()) && i.getValueSupplier() != null).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(validList)) {
            throw new RuntimeException("有效的数据字段定义为空!");
        }
        return validList;
    }

    /**
     * 生成单个JSON对象字符串
     *
     * @param fieldList 数据字段定义
     * @return JSON对象字符串
     */
    private static String generateJson(List<DataField> fieldList) {
        fieldList = skipInvalidFields(fieldList);
        Map<String, Object> element = new LinkedHashMap<>();
        fieldList.forEach(f -> {
            if (f == null || StringUtils.isBlank(f.getField()) || f.getValueSupplier() == null) {
                return;
            }
            element.put(f.getField(), f.getValueSupplier().get());
        });
        try {
            return JsonUtils.toJson(element);
        } catch (Exception e) {
            logger.error("转换JSON字符串时出错", e);
        }
        return null;
    }
}
