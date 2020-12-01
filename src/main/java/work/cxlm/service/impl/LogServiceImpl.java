package work.cxlm.service.impl;

import cn.hutool.core.lang.Assert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import work.cxlm.exception.ForbiddenException;
import work.cxlm.model.dto.LogDTO;
import work.cxlm.model.entity.Log;
import work.cxlm.model.entity.User;
import work.cxlm.model.support.QfzsConst;
import work.cxlm.repository.LogRepository;
import work.cxlm.security.context.SecurityContextHolder;
import work.cxlm.service.LogService;
import work.cxlm.service.UserService;
import work.cxlm.service.base.AbstractCrudService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * created 2020/10/29 15:30
 *
 * @author ryanwang
 * @author cxlm
 */
@Service
public class LogServiceImpl extends AbstractCrudService<Log, Long> implements LogService {

    private final LogRepository logRepository;
    private final UserService userService;

    public LogServiceImpl(LogRepository logRepository,
                          UserService userService) {
        super(logRepository);
        this.logRepository = logRepository;
        this.userService = userService;
    }

    @Override
    public Page<LogDTO> pageClubLatest(int top, Integer clubId) {
        Assert.isTrue(top > 0, "每页条目必须大于 0");
        User admin = SecurityContextHolder.ensureUser();
        Map<Integer, User> userMap = userService.getAllUserMap();  // 因为需要用户信息，一次查询以避免多次数据库 IO
        PageRequest latestPageable = PageRequest.of(0, top, Sort.by(Sort.Direction.DESC, "createTime"));
        if (admin.getRole().isSystemAdmin()) {
            return listAll(latestPageable).map(log -> wrapLogWithHeadAndWho(new LogDTO().convertFrom(log), userMap));
        }
        if (!userService.managerOfClub(admin, clubId)) {
            throw new ForbiddenException("权限不足，禁止操作");
        }
        // 按创建时间降序排序，并取第一页
        return logRepository.findAllByGroupId(clubId, latestPageable).
                map(log -> wrapLogWithHeadAndWho(new LogDTO().convertFrom(log), userMap));
    }

    @Override
    public List<LogDTO> listAllByClubId(Integer clubId) {
        User admin = SecurityContextHolder.ensureUser();
        Map<Integer, User> userMap = userService.getAllUserMap();  // 因为需要用户信息，一次查询以避免多次数据库 IO
        if (admin.getRole().isAdminRole()) {
            return listAll().stream().
                    map(log -> wrapLogWithHeadAndWho(new LogDTO().convertFrom(log), userMap)).
                    collect(Collectors.toList());
        }
        if (!userService.managerOfClub(admin, clubId)) {
            throw new ForbiddenException("权限不足，禁止操作");
        }
        return logRepository.findAllByGroupId(clubId).stream().
                map(log -> wrapLogWithHeadAndWho(new LogDTO().convertFrom(log), userMap)).
                collect(Collectors.toList());
    }

    private LogDTO wrapLogWithHeadAndWho(LogDTO logDTO, Map<Integer, User> userMap) {
        if (logDTO.getLogKey() == null || logDTO.getLogKey() == -1) {
            logDTO.setShowHead(QfzsConst.SYSTEM_HEAD);
            logDTO.setWho("系统");
            return logDTO;
        }
        User targetUser = userMap.get(logDTO.getLogKey());
        if (targetUser == null) {
            logDTO.setWho("不明人士");
            logDTO.setShowHead(QfzsConst.SYSTEM_HEAD);
            return logDTO;
        }
        logDTO.setWho(targetUser.getRealName());
        logDTO.setShowHead(targetUser.getHead());
        return logDTO;
    }
}
