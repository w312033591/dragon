package com.tianshouzhi.dragon.sharding.jdbc.statement;

import com.tianshouzhi.dragon.common.jdbc.statement.DragonStatement;
import com.tianshouzhi.dragon.sharding.jdbc.connection.DragonShardingConnection;
import com.tianshouzhi.dragon.sharding.pipeline.HandlerContext;
import com.tianshouzhi.dragon.sharding.pipeline.Pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.SQLWarning;

/**
 * Created by TIANSHOUZHI336 on 2016/12/11.
 */
public class DragonShardingStatement extends DragonStatement{
    private static final Logger LOGGER= LoggerFactory.getLogger(DragonShardingStatement.class);
    private DragonShardingConnection dragonShardingConnection;
    public DragonShardingStatement(DragonShardingConnection dragonShardingConnection) {
        this.dragonShardingConnection = dragonShardingConnection;
    }
    public DragonShardingStatement(int resultSetType, int resultSetConcurrency, DragonShardingConnection dragonShardingConnection) {
        super(resultSetType,resultSetConcurrency);
        this.dragonShardingConnection = dragonShardingConnection;
    }
    public DragonShardingStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability, DragonShardingConnection dragonShardingConnection) {
        super(resultSetType,resultSetConcurrency,resultSetHoldability);
        this.dragonShardingConnection = dragonShardingConnection;
    }

    @Override
    protected boolean doExecute() throws SQLException {
        Pipeline pipeline = new Pipeline(this, dragonShardingConnection.getRouter());
        pipeline.execute();
        HandlerContext handlerContext = pipeline.getHandlerContext();
        boolean isQuery = handlerContext.isQuery();
        if(!isQuery){
            updateCount=handlerContext.getTotalUpdateCount();
        }else{
            resultSet=handlerContext.getMergedResultSet();
        }
        return isQuery;
    }

    @Override
    public void close() throws SQLException {
        if(resultSet!=null){
            resultSet.close();
        }
    }

    @Override
    public void cancel() throws SQLException {
        checkClosed();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        checkClosed();
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        checkClosed();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        checkClosed();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        checkClosed();
        return false;
    }

    @Override
    public void clearBatch() throws SQLException {
        checkClosed();
    }

    @Override
    public DragonShardingConnection getConnection() throws SQLException {
        checkClosed();
        return dragonShardingConnection;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        checkClosed();
        return false;
    }
}
