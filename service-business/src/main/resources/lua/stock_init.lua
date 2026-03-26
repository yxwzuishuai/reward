-- 库存初始化脚本（仅当key不存在时设置）
-- KEYS[1] = 库存key
-- ARGV[1] = 初始库存数量
-- ARGV[2] = 过期时间(秒), 0表示不过期
-- 返回: 1 设置成功, 0 key已存在

local result = redis.call('SETNX', KEYS[1], tonumber(ARGV[1]))
if result == 1 and tonumber(ARGV[2]) > 0 then
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
end
return result
