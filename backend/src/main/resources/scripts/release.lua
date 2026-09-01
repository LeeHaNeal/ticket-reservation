-- Atomically release one unit of stock previously reserved by a member.
-- Used both for user-initiated cancellation and for compensating a Redis
-- success that failed to persist to the database.
-- KEYS[1] = stock key
-- KEYS[2] = reserved-members set key
-- ARGV[1] = memberId
-- ARGV[2] = totalStock (cap, so a double release can never push stock above the event total)
--
-- Returns: 1 if released, 0 if this member had no active reservation to release

local stockKey = KEYS[1]
local memberKey = KEYS[2]
local memberId = ARGV[1]
local totalStock = tonumber(ARGV[2])

if redis.call('SISMEMBER', memberKey, memberId) == 0 then
    return 0
end

redis.call('SREM', memberKey, memberId)

local stock = tonumber(redis.call('GET', stockKey))
if stock == nil then
    redis.call('SET', stockKey, 1)
elseif stock < totalStock then
    redis.call('INCR', stockKey)
end

return 1
