package com.baiyi.opscloud.account.impl;

import com.baiyi.opscloud.account.IAccount;
import com.baiyi.opscloud.account.base.AccountType;
import com.baiyi.opscloud.account.builder.OcAccountBuilder;
import com.baiyi.opscloud.account.builder.OcUserBuilder;
import com.baiyi.opscloud.account.convert.ZabbixUserConvert;
import com.baiyi.opscloud.common.util.RegexUtils;
import com.baiyi.opscloud.common.util.ZabbixUtils;
import com.baiyi.opscloud.domain.generator.opscloud.OcAccount;
import com.baiyi.opscloud.domain.generator.opscloud.OcUser;
import com.baiyi.opscloud.zabbix.entry.ZabbixUser;
import com.baiyi.opscloud.zabbix.entry.ZabbixUserMedia;
import com.baiyi.opscloud.zabbix.entry.ZabbixUsergroup;
import com.baiyi.opscloud.zabbix.server.ZabbixUserServer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author baiyi
 * @Date 2020/1/6 8:51 下午
 * @Version 1.0
 */
@Component("ZabbixAccount")
public class ZabbixAccount extends BaseAccount implements IAccount {

    public static final String ZABBIX_DEFAULT_USERGROUP = "users_default";
    public static final String ZABBIX_DEFAULT_HOSTGROUP = "group_default";

    @Resource
    private ZabbixUserServer zabbixUserServer;

    @Override
    protected List<OcUser> getUserList() {
        return zabbixUserServer.getAllZabbixUser().stream().map(e -> OcUserBuilder.build(e)).collect(Collectors.toList());
    }

    @Override
    protected List<OcAccount> getOcAccountList() {
        return zabbixUserServer.getAllZabbixUser().stream().map(e -> OcAccountBuilder.build(e)).collect(Collectors.toList());
    }

    @Override
    protected int getAccountType() {
        return AccountType.ZABBIX.getType();
    }

    /**
     * 创建
     *
     * @return
     */
    @Override
    public Boolean create(OcUser user) {
        ZabbixUser zabbixUser = zabbixUserServer.getUser(user.getUsername());
        if (zabbixUser != null) return true;
        return zabbixUserServer.createUser(ZabbixUserConvert.convertOcUser(user), getMediaList(user), getUsrgrps(user));
    }

    private List<Map<String, String>> getUsrgrps(OcUser user) {
        List<Map<String, String>> usrgrps = queryUserServerGroupPermission(user).stream().map(e -> {
            String usergrpName = ZabbixUtils.convertUsergrpName(e.getName());
            ZabbixUsergroup usergroup = zabbixUserServer.getUsergroup(usergrpName);
            Map<String, String> usrgrp = Maps.newHashMap();
            usrgrp.put("usrgrpid", usergroup.getUsrgrpid());
            return usrgrp;
        }).collect(Collectors.toList());

        Map<String, String> defUsrgrp = Maps.newHashMap();
        defUsrgrp.put("usrgrpid", zabbixUserServer.getUsergroup(ZABBIX_DEFAULT_USERGROUP).getUsrgrpid());
        usrgrps.add(defUsrgrp);
        return usrgrps;
    }

    /**
     * 获取用户告警媒介
     *
     * @param user
     * @return
     */
    private List<ZabbixUserMedia> getMediaList(OcUser user) {
        List<ZabbixUserMedia> mediaList = Lists.newArrayList();
        try {
            RegexUtils.isEmail(user.getEmail());
            ZabbixUserMedia mailMedia = ZabbixUserMedia.builder()
                    .mediatypeid(ZabbixUserMedia.MEDIATYPE_MAIL)
                    .sendto(user.getEmail())
                    .build();
            mediaList.add(mailMedia);
        } catch (Exception e) {
        }
        try {
            RegexUtils.isPhone(user.getPhone());
            ZabbixUserMedia mailMedia = ZabbixUserMedia.builder()
                    .mediatypeid(ZabbixUserMedia.MEDIATYPE_PHONE)
                    .sendto(user.getPhone())
                    .build();
            mediaList.add(mailMedia);
        } catch (Exception e) {
        }
        return mediaList;
    }

    /**
     * 移除
     *
     * @return
     */
    @Override
    public Boolean delete(OcUser user) {
        return zabbixUserServer.deleteUser(user.getUsername());
    }

    @Override
    public Boolean update(OcUser user) {
        ZabbixUser checkZabbixUser = zabbixUserServer.getUser(user.getUsername());
        if (checkZabbixUser == null)
            return create(user);
        ZabbixUser zabbixUser = ZabbixUserConvert.convertOcUser(user);
        zabbixUser.setUserid(checkZabbixUser.getUserid());
        return zabbixUserServer.updateUser(zabbixUser, getMediaList(user), getUsrgrps(user));
    }

    @Override
    public Boolean grant(OcUser user, String resource) {
        return update(user);
    }

    @Override
    public Boolean revoke(OcUser user, String resource) {
        return update(user);
    }

    @Override
    public Boolean active(OcUser user, boolean active) {
        return Boolean.TRUE;
    }

}
