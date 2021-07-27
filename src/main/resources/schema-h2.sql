create table PolicyCheckLog
(
  checkId     number(19),
  jobId       number(19),
  status      varchar2(100),
  start       date,
  end         date,
  checkCount  number(19),
  passCount   number(19),
  failedCount number(19),
  checkResult varchar2(100)
)
;

create table PolicyCheckLogError
(
  checkId     number(19),
  topId       number(19),
  message     varchar2(4000)
)
;