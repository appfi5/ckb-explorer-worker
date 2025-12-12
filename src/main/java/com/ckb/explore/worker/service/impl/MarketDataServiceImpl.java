package com.ckb.explore.worker.service.impl;

import static com.ckb.explore.worker.constants.CommonConstantsKey.BOUNTY_ADDRESS_HASH;
import static com.ckb.explore.worker.constants.CommonConstantsKey.BURN_QUOTA;
import static com.ckb.explore.worker.constants.CommonConstantsKey.DEFAULT_UNIT;
import static com.ckb.explore.worker.constants.CommonConstantsKey.ECOSYSTEM_QUOTA;
import static com.ckb.explore.worker.constants.CommonConstantsKey.FOUNDATION_RESERVE_QUOTA;
import static com.ckb.explore.worker.constants.CommonConstantsKey.FOUNDING_PARTNER_QUOTA;
import static com.ckb.explore.worker.constants.CommonConstantsKey.PRIVATE_SALE_QUOTA;
import static com.ckb.explore.worker.constants.CommonConstantsKey.TEAM_QUOTA;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ckb.explore.worker.domain.dto.BlockDaoDto;
import com.ckb.explore.worker.domain.dto.MarketDataDto;
import com.ckb.explore.worker.entity.Script;
import com.ckb.explore.worker.entity.StatisticAddress;
import com.ckb.explore.worker.service.BlockService;
import com.ckb.explore.worker.service.MarketDataService;
import com.ckb.explore.worker.service.ScriptService;
import com.ckb.explore.worker.service.StatisticAddressService;
import com.ckb.explore.worker.utils.CkbUtil;
import com.ckb.explore.worker.utils.CkbUtil.DaoDto;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.nervos.ckb.utils.address.Address;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MarketDataServiceImpl  implements MarketDataService {

  // ============================ 依赖注入 =============================
  @Resource
  private BlockService blockService;

  @Resource
  private ScriptService scriptService;

  @Resource
  private StatisticAddressService statisticAddressService;

  @Value("${ckb.firstReleasedTimestampOther:1593561600000}")
  private Long firstReleasedTimestampOther;

  @Value("${ckb.secondReleasedTimestampOther:1609372800000}")
  private Long secondReleasedTimestampOther;

  @Value("${ckb.thirdReleasedTimestampOther:1672444800000}")
  private Long thirdReleasedTimestampOther;

  @Value("${ckb.firstReleasedTimestampMay:1588291200000}")
  private Long firstReleasedTimestampMay;

  @Value("${ckb.secondReleasedTimestampMay:1619827200000}")
  private Long secondReleasedTimestampMay;

  @Value("${ckb.thirdReleasedTimestampMay:1651363200000}")
  private Long thirdReleasedTimestampMay;

  @Override
  public MarketDataDto getMarketData(Long tipBlockNumber, String unit) {
    unit = unit == null ? DEFAULT_UNIT : unit;
    Long currentTimestamp;
    BlockDaoDto tipBlock;
    MarketDataDto marketData = new MarketDataDto();
    if (tipBlockNumber == null) {
      // 当前UTC时间毫秒数（对应Ruby的Time.find_zone("UTC").now）
      currentTimestamp = LocalDateTime.now(ZoneOffset.UTC)
          .toInstant(ZoneOffset.UTC)
          .toEpochMilli();
      tipBlock = null;
    } else {
      // 取tip_block的timestamp
      tipBlock = blockService.getBlockDaoDtoByBlockNumber(tipBlockNumber);
      currentTimestamp = (tipBlock != null) ? tipBlock.getTimestamp() : 0L;
    }

    var parsedDao = parsedDao(tipBlock);
    var ecosystemLocked = ecosystemLocked(currentTimestamp);
    var teamLocked = teamLocked(currentTimestamp);
    var privateSaleLocked = privateSaleLocked(currentTimestamp);
    var foundingPartnersLocked = foundingPartnersLocked(currentTimestamp);
    var foundationReserveLocked = foundationReserveLocked(currentTimestamp);
    var bugBountyLocked = bugBountyLocked();
    var circulatingSupply = circulatingSupply(parsedDao, unit, ecosystemLocked, teamLocked, privateSaleLocked,
      foundingPartnersLocked, foundationReserveLocked, bugBountyLocked);
    marketData.setEcosystemLocked(ecosystemLocked);
    marketData.setTeamLocked(teamLocked);
    marketData.setPrivateSaleLocked(privateSaleLocked);
    marketData.setFoundingPartnersLocked(foundingPartnersLocked);
    marketData.setFoundationReserveLocked(foundationReserveLocked);
    marketData.setBugBountyLocked(bugBountyLocked);
    marketData.setCirculatingSupply(circulatingSupply);
    marketData.setParsedDao(parsedDao);
    return marketData;
  }

  /**
   * 生态系统锁定金额（对应Ruby的ecosystem_locked）
   */
  public BigInteger ecosystemLocked(Long now) {
    Long first = firstReleasedTimestampOther;
    Long second = secondReleasedTimestampOther;
    Long third = thirdReleasedTimestampOther;

    if (now < first) {
      return ECOSYSTEM_QUOTA.multiply(BigInteger.valueOf(95)).divide(BigInteger.valueOf(100));
    } else if (now >= first && now < second) {
      return ECOSYSTEM_QUOTA.multiply(BigInteger.valueOf(75)).divide(BigInteger.valueOf(100));
    } else if (now >= second && now < third) {
      return ECOSYSTEM_QUOTA.multiply(BigInteger.valueOf(50)).divide(BigInteger.valueOf(100));
    } else {
      return BigInteger.ZERO;
    }
  }

  /**
   * 团队锁定金额（对应Ruby的team_locked）
   */
  public BigInteger teamLocked(Long now) {
    Long first = firstReleasedTimestampMay;
    Long second = secondReleasedTimestampMay;
    Long third = thirdReleasedTimestampMay;

    if (now < first) {
      return TEAM_QUOTA.multiply(new BigInteger("2").divide(new BigInteger("3")));
    } else if (now >= first && now < second) {
      return TEAM_QUOTA.multiply(new BigInteger("5").divide(new BigInteger("10")));
    } else if (now >= second && now < third) {
      return TEAM_QUOTA.multiply(new BigInteger("1").divide(new BigInteger("3")));
    } else {
      return BigInteger.ZERO;
    }
  }

  /**
   * 私募锁定金额（对应Ruby的private_sale_locked）
   */
  public BigInteger privateSaleLocked(Long currentTimestamp) {
    if (currentTimestamp < firstReleasedTimestampMay) {
      return PRIVATE_SALE_QUOTA.multiply(new BigInteger("1").divide(new BigInteger("3")));
    }
    return BigInteger.ZERO;
  }

  /**
   * 创始合伙人锁定金额（对应Ruby的founding_partners_locked）
   */
  public BigInteger foundingPartnersLocked(Long now) {
    Long first = firstReleasedTimestampMay;
    Long second = secondReleasedTimestampMay;
    Long third = thirdReleasedTimestampMay;

    if (now < first) {
      return FOUNDING_PARTNER_QUOTA;
    } else if (now >= first && now < second) {
      return FOUNDING_PARTNER_QUOTA.multiply(new BigInteger("75").divide(new BigInteger("100")));
    } else if (now >= second && now < third) {
      return FOUNDING_PARTNER_QUOTA.multiply(new BigInteger("5").divide(new BigInteger("10")));
    } else {
      return BigInteger.ZERO;
    }
  }

  /**
   * 基金会储备锁定金额（对应Ruby的foundation_reserve_locked）
   */
  public BigInteger foundationReserveLocked(Long currentTimestamp) {
    if (currentTimestamp < firstReleasedTimestampOther) {
      return FOUNDATION_RESERVE_QUOTA;
    }
    return BigInteger.ZERO;
  }

  /**
   * Bug bounty锁定金额（对应Ruby的bug_bounty_locked）
   */
  public BigInteger bugBountyLocked() {

    // 计算地址的哈希
    var addressScriptHash = Address.decode(BOUNTY_ADDRESS_HASH).getScript().computeHash();
    LambdaQueryWrapper<Script> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(Script::getScriptHash, addressScriptHash);
    var script = scriptService.getOne(queryWrapper);
    if(script == null){
      return BigInteger.ZERO;
    }
    var addressStatistics = statisticAddressService.getOne(
        new LambdaQueryWrapper<StatisticAddress>()
            .eq(StatisticAddress::getLockScriptId, script.getId()));
    if(addressStatistics == null){
      return BigInteger.ZERO;
    }
    // 余额转为整数（对应Ruby的to_i），默认0
    return addressStatistics.getBalance();
  }

  /**
   * 解析DAO数据（对应Ruby的parsed_dao）
   */
  private DaoDto parsedDao(BlockDaoDto tipBlock) {
    DaoDto parsedDao;
    if (tipBlock == null || tipBlock.getDao() == null) {
      parsedDao = new DaoDto(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO);
    } else {
      // 调用工具类解析DAO（对应Ruby的CkbUtils.parse_dao）
      parsedDao = CkbUtil.parseDao(tipBlock.getDao());
    }
    return parsedDao;
  }

  /**
   * 总供应量（对应Ruby的total_supply）
   */
  public BigDecimal totalSupply(Long currentTimestamp, DaoDto dao, BigInteger unmadeDaoInterests, String unit) {
    BigInteger ci = dao.getCI();
    BigInteger si = dao.getSI();
    Long firstMay = firstReleasedTimestampMay;
    BigInteger result;

    if (currentTimestamp > firstMay) {
      // 公式：c_i - BURN_QUOTA - (s_i - unmade_dao_interests)
      result = ci.subtract(BURN_QUOTA)
          .subtract(si.subtract(unmadeDaoInterests));
    } else {
      // 公式：c_i - BURN_QUOTA
      result = ci.subtract(BURN_QUOTA);
    }

    // 按单位转换（ckb单位需除以10^8，截断8位小数）
    return convertUnit(new BigDecimal( result), unit);
  }

  /**
   * 流通供应量（对应Ruby的circulating_supply）
   */
  public BigDecimal circulatingSupply(DaoDto dao, String unit,BigInteger ecosystemLocked,BigInteger teamLocked,
      BigInteger privateSaleLocked, BigInteger foundingPartnersLocked, BigInteger foundationReserveLocked, BigInteger bugBountyLocked) {
    BigInteger ci = dao.getCI();
    BigInteger si = dao.getSI();

    // 公式：c_i - s_i - BURN_QUOTA - 各锁定金额
    BigInteger result = ci.subtract(si)
        .subtract(BURN_QUOTA)
        .subtract(ecosystemLocked)
        .subtract(teamLocked)
        .subtract(privateSaleLocked)
        .subtract(foundingPartnersLocked)
        .subtract(foundationReserveLocked)
        .subtract(bugBountyLocked);

    // 按单位转换
    return convertUnit(new BigDecimal( result), unit);
  }

  /**
   * 单位转换（对应Ruby的除以10^8并truncate(8)）
   */
  private BigDecimal convertUnit(BigDecimal value, String unit) {
    if ("ckb".equals(unit)) {
      // 除以10^8，截断到8位小数（对应Ruby的truncate(8)）
      return value.divide(new BigDecimal("10").pow(8), 8, RoundingMode.DOWN);
    }
    return value;
  }

}
