package com.tianshouzhi.dragon.sharding.route;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;

/**
 * 每个逻辑表 管理了 物理表 ，每个物理表 对应一个读写分离数据源编号
 */
public class LogicTable extends LogicConfig{
    private LogicDatabase logicDatabase;
    private final Set<RouteRule> dbRouteRules;
    private String logicTableName;
    private final Set<RouteRule> tbRouteRules;//eg:${user_id}.toLong().intdiv(100)%100
    /**真实库和表的对应关系，可以不设置，但是如果不设置的话，无法从所有分库进行查询*/
    private Map<String,List<String>> realDBTBMap;

    /**
     * @param logicTableName
     * @param tableNameFormat
     * @param tbRouteRuleStrs
     * @param logicDatabase
     * @param realDBTBMap
     */
    public LogicTable(String logicTableName,String tableNameFormat, Set<String> tbRouteRuleStrs, Set<String> dbRouteRuleStrs,LogicDatabase logicDatabase,Map<String,List<String>> realDBTBMap) {
        super(tableNameFormat);
        if(CollectionUtils.isEmpty(tbRouteRuleStrs)){
            throw new RuntimeException("tbRouteRuleStrList can't be empty!!!");
        }
        if(logicDatabase==null){
            throw new NullPointerException();
        }

        this.tbRouteRules = new HashSet<RouteRule>();
        for (String tbRouteRule : tbRouteRuleStrs) {
            RouteRule routeRule = new RouteRule(tbRouteRule);
            this.tbRouteRules.add(routeRule);
        }
        this.dbRouteRules = new HashSet<RouteRule>();
        for (String dbRouteRule : dbRouteRuleStrs) {
            RouteRule routeRule = new RouteRule(dbRouteRule);
            this.dbRouteRules.add(routeRule);
        }
        this.logicTableName =logicTableName;
        this.logicDatabase=logicDatabase;
        this.realDBTBMap = realDBTBMap;
    }

    /**
     * 根据路由参数，获取真实库名
     * @param shardColumnValuesMap
     * @return
     */
    public String getRealDBName(Map<String,Object> shardColumnValuesMap){
        Long realDBIndex = getRealIndex(shardColumnValuesMap,dbRouteRules);
        return logicDatabase.format(realDBIndex);
    }

    public String getRealTBName(Map<String,Object> shardColumnValuesMap){
        Long realTBIndex=getRealIndex(shardColumnValuesMap,tbRouteRules);
        return format(realTBIndex);
    }


    /**
     * 根据真实库名解析Index 例如 user_1010，解析后的值为1010
     * @param realTBName
     * @return
     */
    public Long parseRealTBIndex(String realTBName) {
        return super.parseIndex(realTBName);
    }
    public Long parseRealDBIndex(String realDBName) {
        return logicDatabase.parseIndex(realDBName);
    }

    /**
     * db 和tb 使用到的所有分区字段 ，用于判断sql中的某一列的值，是否可以作为分区字段
     * @return
     */
    public boolean isShardColumn(String column) {
        for (RouteRule tbRouteRule : tbRouteRules) {
            if(tbRouteRule.getShardColumns().contains(column)){
                return true;
            }
        }
        for (RouteRule dbRouteRule : dbRouteRules) {
            if(dbRouteRule.getShardColumns().contains(column)){
                return true;
            }
        }
        return false;
    }

    public String getLogicTableName() {
        return logicTableName;
    }

    public Map<String, List<String>> getRealDBTBMap() {
        return realDBTBMap;
    }

    /**
     * 根据路由参数计算真实编号
     * @param params
     * @return
     */
    protected Long getRealIndex(Map<String, Object> params,Set<RouteRule> routeRules) {
        if(params==null){
            throw new NullPointerException();
        }
        RouteRule selectedRouteRule=null;
        for (RouteRule routeRule : routeRules) {
            if(params.keySet().containsAll(routeRule.getShardColumns())){
                selectedRouteRule=routeRule;
                break;
            }
        }
        if(selectedRouteRule==null){
            throw new RuntimeException("no matched route rule found !!!");
        }

        return (Long) DragonGroovyEngine.eval(selectedRouteRule.getReplacedRouteRuleStr(), params);
    }

    protected static class RouteRule{
        private String originRouteRuleStr;//eg:${user_id}.toLong().intdiv(100)%100
        private String replacedRouteRuleStr;//eg:user_id.toLong().intdiv(100)%100
        private List<String> shardColumns;

        public RouteRule(String originRouteRuleStr) {
            if(StringUtils.isBlank(originRouteRuleStr)){
                throw new IllegalArgumentException("'originRouteRuleStr' can't be blank");
            }

            this.originRouteRuleStr = originRouteRuleStr;
            this.shardColumns = new ArrayList<String>();
            Matcher matcher = routeRuleVariablePattern.matcher(originRouteRuleStr);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String varible = matcher.group(1);// 脚本中的变量名${xxx}
                String column = varible.substring(varible.indexOf("{")+1,varible.indexOf("}"));//变量名：xxx
                shardColumns.add(column);
                matcher.appendReplacement(sb,column);
            }
            if(CollectionUtils.isEmpty(shardColumns)){
                throw new IllegalArgumentException("'originRouteRuleStr' must contains shard column!!!");
            }
            matcher.appendTail(sb);
            this. replacedRouteRuleStr=sb.toString();
        }

        public String getOriginRouteRuleStr() {
            return originRouteRuleStr;
        }

        public String getReplacedRouteRuleStr() {
            return replacedRouteRuleStr;
        }

        public List<String> getShardColumns() {
            return shardColumns;
        }
    }
}
