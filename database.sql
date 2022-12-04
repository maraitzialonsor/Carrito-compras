create database servicio_carrito;

create table articulos
(
    id_articulo integer auto_increment primary key,
    nombre varchar(50) not null,
    descripcion varchar(100) not null,
    precio float not null,
    cantidad integer not null,
    foto longblob not null
);

create table carrito_compras
(
    id_carrito_compra integer auto_increment primary key,
    id_articulo integer not null,
    cantidad integer not null
);

alter table carrito_compras add foreign key(id_articulo) references articulos(id_articulo);

insert into articulos (id_articulo, nombre, descripcion, precio, cantidad, foto) 
values (0, 'Jitomate', 'Jitomate saladet', 20.3, 50, 0x89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082);

insert into carrito_compras (id_carrito_compra, id_articulo, cantidad)
values (0, 1, 3);

SELECT a.nombre, a.precio, a.foto from articulos a where a.nombre LIKE '%saladet%' OR a.descripcion LIKE '%saladet%';