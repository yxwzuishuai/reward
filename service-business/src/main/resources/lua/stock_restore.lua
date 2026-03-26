-- 库存回滚脚本（订单取消/支付超时）
-- KEYS[1] = 库存key
-- ARGV[1] = 恢复数量
-- 返回: 恢复后的库存数量, -1 key不存在

local stock = redis.call('GET', KEYS[1])
if stock == false then
    return -1
end

redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))
return tonumber(stock) + tonumber(ARGV[1])
