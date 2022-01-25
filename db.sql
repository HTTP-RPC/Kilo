drop schema if exists demo;

create schema demo;

use demo;

create table pet (
  name varchar(20),
  owner varchar(20),
  species varchar(20),
  sex char(1),
  birth date,
  death date
);

insert into pet (name, owner, species, sex, birth, death) values ('Fluffy', 'Harold', 'cat', 'f', '1993-02-04', null);
insert into pet (name, owner, species, sex, birth, death) values ('Claws', 'Gwen', 'cat', 'm', '1994-03-17', null);
insert into pet (name, owner, species, sex, birth, death) values ('Buffy', 'Harold', 'dog', 'f', '1989-05-13', null);
insert into pet (name, owner, species, sex, birth, death) values ('Fang', 'Benny', 'dog', 'm', '1990-08-27', null);
insert into pet (name, owner, species, sex, birth, death) values ('Bowser', 'Diane', 'dog', 'm', '1979-08-31', '1995-07-29');
insert into pet (name, owner, species, sex, birth, death) values ('Chirpy', 'Gwen', 'bird', 'f', '1998-09-11', null);
insert into pet (name, owner, species, sex, birth, death) values ('Whistler', 'Gwen', 'bird', null, '1997-12-09', null);
insert into pet (name, owner, species, sex, birth, death) values ('Slim', 'Benny', 'snake', 'm', '1996-04-29', null);

create table item (
    id int not null auto_increment,
    description varchar(256) not null,
    price double not null,
    primary key (id)
);

drop user if exists 'demo'@'%';

create user 'demo'@'%' identified by 'demo123!';
grant select, insert, update, delete on demo.* to 'demo'@'%';
flush privileges;
