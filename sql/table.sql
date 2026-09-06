-- 用户表
create table if not exists user
(
    id             bigint auto_increment primary key comment 'id',
    user_account   varchar(256)                           not null comment '账号',
    user_password  varchar(512)                           not null comment '密码',
    user_name      varchar(256)                           null comment '用户昵称',
    user_avatar    varchar(1024)                          null comment '用户头像',
    user_profile   varchar(512)                           null comment '用户简介',
    user_role      varchar(256) default 'user'            not null comment '用户角色: user/admin',
    vip_expire_time datetime                               null comment '会员过期时间',
    vip_code       varchar(128)                           null comment '会员兑换码',
    vip_number     bigint                                 null comment '会员编号',
    share_code     varchar(64)                            null comment '分享码',
    invite_user    bigint                                 null comment '邀请用户 id',
    edit_time      datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    create_time    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete      tinyint      default 0                 not null comment '是否删除'
) comment '用户';
create index idx_user_name on user (user_name);
create unique index uk_user_account on user (user_account);

-- 空间表
create table if not exists space
(
    id          bigint auto_increment primary key comment 'id',
    space_name  varchar(128)                           null comment '空间名称',
    space_level int      default 0                     null comment '空间级别: 0-普通版, 1-专业版, 2-旗舰版',
    max_size    bigint   default 0                     null comment '空间图片的最大总大小',
    max_count   bigint   default 0                     null comment '空间图片的最大数量',
    total_size  bigint   default 0                     null comment '当前空间的图片总大小',
    total_count bigint   default 0                     null comment '当前空间的图片数量',
    user_id     bigint                                 not null comment '创建用户 id',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    edit_time   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '是否删除'
) comment '空间';
create index idx_user_id on space (user_id);
create index idx_space_name on space (space_name);

-- 图片表
create table if not exists picture
(
    id             bigint auto_increment primary key comment 'id',
    url            varchar(1024)                          not null comment '图片 url',
    thumbnail_url  varchar(1024)                          null comment '缩略图 url',
    name           varchar(128)                           not null comment '图片名称',
    introduction   varchar(512)                           null comment '简介',
    category       varchar(64)                            null comment '分类',
    tags           varchar(512)                           null comment '标签(JSON 数组)',
    pic_size       bigint                                 null comment '图片体积',
    pic_width      int                                    null comment '图片宽度',
    pic_height     int                                    null comment '图片高度',
    pic_scale      double                                 null comment '图片宽高比例',
    pic_format     varchar(32)                            null comment '图片格式',
    user_id        bigint                                 not null comment '创建用户 id',
    review_status  int      default 0                     not null comment '审核状态: 0-待审核, 1-通过, 2-拒绝',
    review_message varchar(512)                           null comment '审核信息',
    reviewer_id    bigint                                 null comment '审核人 id',
    review_time    datetime                               null comment '审核时间',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    edit_time      datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete      tinyint  default 0                 not null comment '是否删除'
) comment '图片';
create index idx_category on picture (category);
create index idx_name on picture (name);
create index idx_review_status on picture (review_status);
create index idx_user_id on picture (user_id);
create index idx_tags on picture (tags);
