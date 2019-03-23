create database test;
use test;
create table bulk_upload_test (
  id int not null auto_increment,
  text1 varchar(32),
  text2 varchar(16),
  number1 int,
  number2 decimal(5, 2),
  number3 decimal(10, 4),
  primary key (id)
);
