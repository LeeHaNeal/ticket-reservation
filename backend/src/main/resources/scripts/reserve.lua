-- Atomically reserve one unit of stock for a member (FCFS / first-come-first-served gate).
-- KEYS[1] = stock key, e.g. "event:{id}:stock"
-- KEYS[2] = reserved-members set key, e.g. "event:{id}:members"
-- ARGV[1] = memberId
--
-- Returns:
--  -3  stock key missing (cache not initialized) -> caller should hydrate cache and retry
--  -2  this member already holds a reservation for this event
--  -1  sold out
--  >=0 remaining stock AFTER this reservation succeeded

local stockKey = KEYS[1]
local memberKey = KEYS[2]
local memberId = ARGV[1]

if redis.call('EXISTS', stockKey) == 0 then
    return -3
end

if redis.call('SISMEMBER', memberKey, memberId) == 1 then
    return -2
end

local stock = tonumber(redis.call('GET', stockKey))
if stock <= 0 then
    return -1
end

redis.call('DECR', stockKey)
redis.call('SADD', memberKey, memberId)
return stock - 1
