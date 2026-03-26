-- 库存扣减脚本
-- KEYS[1] = 库存key (如 stock:product:1001)
-- ARGV[1] = 扣减数量
-- 返回: >=0 剩余库存, -1 key不存在, -2 库存不足

local stock = redis.call('GET', KEYS[1])
if stock == false then
    return -1
end

stock = tonumber(stock)
local quantity = tonumber(ARGV[1])

if stock < quantity then
    return -2
end

redis.call('DECRBY', KEYS[1], quantity)
return stock - quantity
