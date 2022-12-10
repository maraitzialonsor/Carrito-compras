package servicio_json;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;

import java.sql.*;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import com.google.gson.*;

// la URL del servicio web es http://localhost:8080/Servicio/rest/ws
// donde:
//	"Servicio" es el dominio del servicio web (es decir, el nombre de archivo Servicio.war)
//	"rest" se define en la etiqueta <url-pattern> de <servlet-mapping> en el archivo WEB-INF\web.xml
//	"ws" se define en la siguiente anotacin @Path de la clase Servicio

@Path("ws")
public class Servicio {
  static DataSource pool = null;
  static {
    try {
      Context ctx = new InitialContext();
      pool = (DataSource) ctx.lookup("java:comp/env/jdbc/datasource_Servicio");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static Gson j = new GsonBuilder().registerTypeAdapter(byte[].class, new AdaptadorGsonBase64())
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").create();

  @POST
  @Path("alta_articulo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response alta(String json) throws Exception {
    ParamAltaArticulo p = (ParamAltaArticulo) j.fromJson(json, ParamAltaArticulo.class);
    Articulo articulo = p.articulo;

    Connection conexion = pool.getConnection();

    if (articulo.nombre == null || articulo.nombre.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el nombre"))).build();

    if (articulo.descripcion == null || articulo.descripcion.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar la descripcion"))).build();

    if (articulo.precio == null || articulo.precio.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el precio"))).build();

    if (articulo.cantidad_almacen == null)
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar la cantidad en almacen"))).build();

    try {
      PreparedStatement stmt_1 = conexion.prepareStatement(
          "INSERT INTO articulos(id_articulo, nombre, descripcion, precio, cantidad, foto) VALUES (0,?,?,?,?,?)");
      // insert into articulos (id_articulo, nombre, descripcion, precio, cantidad,
      // foto)
      // values (0,?,?,?,?,?);

      try {
        stmt_1.setString(1, articulo.nombre);
        stmt_1.setString(2, articulo.descripcion);
        stmt_1.setFloat(3, articulo.precio);
        stmt_1.setInt(4, articulo.cantidad_almacen);
        stmt_1.setBytes(5, articulo.foto);

        ResultSet rs = stmt_1.executeQuery();

      } catch (Exception e) {
        return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
      } finally {
        stmt_1.close();
        conexion.close();
      }
      return Response.ok().build();
    } catch (Exception e) {
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
  }

  @POST
  @Path("consulta_articulos")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response consultaArticulos(String json) throws Exception {
    ParamConsultaArticulos p = (ParamConsultaArticulos) j.fromJson(json, ParamConsultaArticulos.class);
    String palabra = p.article_w;

    Connection conexion = pool.getConnection();

    try {
      PreparedStatement stmt_1 = conexion.prepareStatement(
          "SELECT a.id_articulo, a.nombre, a.precio, a.foto from articulos a where a.nombre LIKE %?% OR a.descripcion LIKE %?%");
      try {
        stmt_1.setString(1, p.article_w);
        stmt_1.setString(2, p.article_w);

        ResultSet rs = stmt_1.executeQuery();
        ArrayList<Articulo> listaArticulos = new ArrayList<Articulo>();
        try {
          while (rs.next()) {
            Articulo r = new Articulo();
            r.id_articulo = rs.getInt(1);
            r.nombre = rs.getString(2);
            r.precio = rs.getFloat(3);
            r.foto = rs.getBytes(4);
            listaArticulos.add(r);
            return Response.ok().entity(j.toJson(r)).build();
          }
          return Response.status(400).entity(j.toJson(new Error("No se encontraron coincidencias"))).build();
        } finally {
          rs.close();
        }
      } finally {
        stmt_1.close();
      }
    } catch (Exception e) {
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    } finally {
      conexion.close();
    }
  }

  @POST
  @Path("descripcion_articulo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response consultaArticulo(String json) throws Exception {
    ParamConsultaDescripcion p = (ParamConsultaDescripcion) j.fromJson(json,
        ParamConsultaDescripcion.class);
    Integer id_articulo = p.id_articulo;

    Connection conexion = pool.getConnection();

    try {
      PreparedStatement stmt_1 = conexion.prepareStatement(
          "SELECT a.descripcion FROM articulos a WHERE id_articulo=?");
      try {
        stmt_1.setInt(1, id_articulo);

        ResultSet rs = stmt_1.executeQuery();
        try {
          if (rs.next()) {
            Articulo r = new Articulo();
            r.descripcion = rs.getString(1);
            return Response.ok().entity(j.toJson(r)).build();
          }
          return Response.status(400).entity(j.toJson(new Error("El articulo no existe"))).build();
        } finally {
          rs.close();
        }
      } finally {
        stmt_1.close();
      }
    } catch (Exception e) {
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    } finally {
      conexion.close();
    }
  }

  @POST
  @Path("compra_articulo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response compraArticulo(String json) throws Exception {
    ParamCompraArticulo p = (ParamCompraArticulo) j.fromJson(json, ParamCompraArticulo.class);
    Integer id_articulo = p.id_articulo;
    Integer cantidad = p.cantidad;
    Integer cantidadStockActualizada=null;
    Integer cantidadCarrito=null;

    Connection conexion = pool.getConnection();

    if (id_articulo == null || id_articulo.equals(""))
      return Response.status(400).entity(j.toJson(new Error("El id de articulo esta vacio"))).build();

    if (cantidad == null || cantidad.equals(""))
      return Response.status(400).entity(j.toJson(new Error("La cantidad está vacía"))).build();

    try {
      conexion.setAutoCommit(false);

      PreparedStatement stmt_1 = conexion.prepareStatement("SELECT a.cantidad FROM articulos a WHERE id_articulo= ?");

      try {
        stmt_1.setInt(0, cantidad);

        ResultSet rs = stmt_1.executeQuery();
        if (rs.next()) {
          Integer cantidadStock = rs.getInt(1);
          if (cantidadStock > cantidad) {
            cantidadStockActualizada = cantidadStock - cantidad;
            cantidadCarrito = cantidad;
            return Response.ok().entity(j.toJson(rs)).build();
          } else {
            cantidadCarrito = cantidadStock;
            return Response.status(400).entity(j.toJson(new Error("La cantidad deseada no está disponible en stock")))
                .build();
          }
        }
        stmt_1.executeUpdate();
      } finally {
        stmt_1.close();
      }

      // Insertar en el carrito de compras
      PreparedStatement stmt_2 = conexion.prepareStatement(
          "insert into carrito_compras (id_carrito_compra, id_articulo, cantidad) values (0, ?, ?)");
      try {
        stmt_2.setInt(1, id_articulo);
        stmt_2.setInt(2, cantidadCarrito);
        stmt_2.executeUpdate();
      } finally {
        stmt_2.close();
      }

      PreparedStatement stmt_3 = conexion.prepareStatement(
          "UPDATE articulos SET cantidad=? where id_articulo=?");
      try {
        stmt_3.setInt(1, cantidadStockActualizada);
        stmt_3.setInt(2, id_articulo);
        stmt_3.executeUpdate();
      } finally {
        stmt_3.close();
      }

      conexion.commit();
    } catch (Exception e) {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    } finally {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();
  }

  // Despliega articulos en el carrito
  @POST
  @Path("consulta_articulos_carrito")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response consultaArticulosCarrito(String json) throws Exception {
    Connection conexion = pool.getConnection();

    try {
      PreparedStatement stmt_1 = conexion.prepareStatement(
          "SELECT a.id_articulo, a.nombre, a.precio, a.foto from articulos a LEFT OUTER JOIN carrito_compras b ON a.id_articulo=b.id_articulo");
      try {
        ResultSet rs = stmt_1.executeQuery();
        ArrayList<Articulo> listaArticulosCarrito = new ArrayList<Articulo>();
        try {
          while (rs.next()) {
            Articulo r = new Articulo();
            r.id_articulo = rs.getInt(1);
            r.nombre = rs.getString(2);
            r.precio = rs.getFloat(3);
            r.foto = rs.getBytes(4);
            listaArticulosCarrito.add(r);
            return Response.ok().entity(j.toJson(r)).build();
          }
          return Response.status(400).entity(j.toJson(new Error("El carrito de compras está vacío"))).build();
        } finally {
          rs.close();
        }
      } finally {
        stmt_1.close();
      }
    } catch (Exception e) {
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    } finally {
      conexion.close();
    }
  }

  //Eliminar articulo de la tabla carrito de compras 

  @POST
  @Path("borra_articulo_carrito")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response borraArticuloCarrito(String json) throws Exception {
    ParamBorraArticuloCarrito p = (ParamBorraArticuloCarrito) j.fromJson(json, ParamBorraArticuloCarrito.class);
    Integer id_carrito_compra = p.id_carrito_compra;
    Integer cantidadStockActualizada;
    Integer cantidadCarrito;
    Integer id_articulo;

    Connection conexion = pool.getConnection();

    try {
      conexion.setAutoCommit(false);

      PreparedStatement stmt_1 = conexion.prepareStatement("SELECT a.cantidad, a.id_articulo FROM carrito_compras a WHERE id_carrito_compra=?");

      try {
        stmt_1.setInt(1, id_carrito_compra);

        ResultSet rs = stmt_1.executeQuery();
        cantidadCarrito = rs.getInt(1);
        id_articulo=rs.getInt(2);

        stmt_1.executeUpdate();
      } finally {
        stmt_1.close();
      }

      // Insertar en el carrito de compras
      PreparedStatement stmt_2 = conexion.prepareStatement(
          "UPDATE articulos SET cantidad=? where id_articulo=?");
      try {
        stmt_2.setInt(1, cantidadCarrito);
        stmt_2.setInt(2, id_articulo);
        stmt_2.executeUpdate();
      } finally {
        stmt_2.close();
      }

      PreparedStatement stmt_3 = conexion.prepareStatement(
          "DELETE FROM carrito_compras where id_carrito_compra=?");
      try {
        stmt_3.setInt(1, id_carrito_compra);
        stmt_3.executeUpdate();
      } finally {
        stmt_3.close();
      }

      conexion.commit();
    } catch (Exception e) {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    } finally {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();
  }

  @POST
  @Path("alta_usuario")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response alta(String json) throws Exception
  {
    ParamAltaUsuario p = (ParamAltaUsuario) j.fromJson(json,ParamAltaUsuario.class);
    Usuario usuario = p.usuario;

    Connection conexion = pool.getConnection();

    if (usuario.email == null || usuario.email.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el email"))).build();

    if (usuario.nombre == null || usuario.nombre.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el nombre"))).build();

    if (usuario.apellido_paterno == null || usuario.apellido_paterno.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el apellido paterno"))).build();

    if (usuario.fecha_nacimiento == null)
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar la fecha de nacimiento"))).build();

    try
    {
      conexion.setAutoCommit(false);

      PreparedStatement stmt_1 = conexion.prepareStatement("INSERT INTO usuarios(id_usuario,email,nombre,apellido_paterno,apellido_materno,fecha_nacimiento,telefono,genero) VALUES (0,?,?,?,?,?,?,?)");
 
      try
      {
        stmt_1.setString(1,usuario.email);
        stmt_1.setString(2,usuario.nombre);
        stmt_1.setString(3,usuario.apellido_paterno);

        if (usuario.apellido_materno != null)
          stmt_1.setString(4,usuario.apellido_materno);
        else
          stmt_1.setNull(4,Types.VARCHAR);

        stmt_1.setTimestamp(5,usuario.fecha_nacimiento);

        if (usuario.telefono != null)
          stmt_1.setLong(6,usuario.telefono);
        else
          stmt_1.setNull(6,Types.BIGINT);

        stmt_1.setString(7,usuario.genero);
        stmt_1.executeUpdate();
      }
      finally
      {
        stmt_1.close();
      }

      if (usuario.foto != null)
      {
        PreparedStatement stmt_2 = conexion.prepareStatement("INSERT INTO fotos_usuarios(id_foto,foto,id_usuario) VALUES (0,?,(SELECT id_usuario FROM usuarios WHERE email=?))");
        try
        {
          stmt_2.setBytes(1,usuario.foto);
          stmt_2.setString(2,usuario.email);
          stmt_2.executeUpdate();
        }
        finally
        {
          stmt_2.close();
        }
      }
      conexion.commit();
    }
    catch (Exception e)
    {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally
    {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();
  }

  /*
   * @POST
   * 
   * @Path("consulta_usuario")
   * 
   * @Consumes(MediaType.APPLICATION_JSON)
   * 
   * @Produces(MediaType.APPLICATION_JSON)
   * public Response consulta(String json) throws Exception {
   * ParamConsultaArticulos p = (ParamConsultaArticulos) j.fromJson(json,
   * ParamConsultaArticulos.class);
   * String email = p.email;
   * 
   * Connection conexion = pool.getConnection();
   * 
   * try {
   * PreparedStatement stmt_1 = conexion.prepareStatement(
   * "SELECT a.email,a.nombre,a.apellido_paterno,a.apellido_materno,a.fecha_nacimiento,a.telefono,a.genero,b.foto FROM usuarios a LEFT OUTER JOIN fotos_usuarios b ON a.id_usuario=b.id_usuario WHERE email=?"
   * );
   * try {
   * stmt_1.setString(1, email);
   * 
   * ResultSet rs = stmt_1.executeQuery();
   * try {
   * if (rs.next()) {
   * Articulo r = new Articulo();
   * r.email = rs.getString(1);
   * r.nombre = rs.getString(2);
   * r.apellido_paterno = rs.getString(3);
   * r.apellido_materno = rs.getString(4);
   * r.fecha_nacimiento = rs.getTimestamp(5);
   * r.telefono = rs.getObject(6) != null ? rs.getLong(6) : null;
   * r.genero = rs.getString(7);
   * r.foto = rs.getBytes(8);
   * return Response.ok().entity(j.toJson(r)).build();
   * }
   * return Response.status(400).entity(j.toJson(new
   * Error("El email no existe"))).build();
   * } finally {
   * rs.close();
   * }
   * } finally {
   * stmt_1.close();
   * }
   * } catch (Exception e) {
   * return Response.status(400).entity(j.toJson(new
   * Error(e.getMessage()))).build();
   * } finally {
   * conexion.close();
   * }
   * }
   * 
   * @POST
   * 
   * @Path("modifica_usuario")
   * 
   * @Consumes(MediaType.APPLICATION_JSON)
   * 
   * @Produces(MediaType.APPLICATION_JSON)
   * public Response modifica(String json) throws Exception {
   * ParamModificaArticulo p = (ParamModificaArticulo) j.fromJson(json,
   * ParamModificaArticulo.class);
   * Articulo usuario = p.usuario;
   * 
   * Connection conexion = pool.getConnection();
   * 
   * if (usuario.email == null || usuario.email.equals(""))
   * return Response.status(400).entity(j.toJson(new
   * Error("Se debe ingresar el email"))).build();
   * 
   * if (usuario.nombre == null || usuario.nombre.equals(""))
   * return Response.status(400).entity(j.toJson(new
   * Error("Se debe ingresar el nombre"))).build();
   * 
   * if (usuario.apellido_paterno == null || usuario.apellido_paterno.equals(""))
   * return Response.status(400).entity(j.toJson(new
   * Error("Se debe ingresar el apellido paterno"))).build();
   * 
   * if (usuario.fecha_nacimiento == null)
   * return Response.status(400).entity(j.toJson(new
   * Error("Se debe ingresar la fecha de nacimiento"))).build();
   * 
   * conexion.setAutoCommit(false);
   * try {
   * PreparedStatement stmt_1 = conexion.prepareStatement(
   * "UPDATE usuarios SET nombre=?,apellido_paterno=?,apellido_materno=?,fecha_nacimiento=?,telefono=?,genero=? WHERE email=?"
   * );
   * try {
   * stmt_1.setString(1, usuario.nombre);
   * stmt_1.setString(2, usuario.apellido_paterno);
   * 
   * if (usuario.apellido_materno != null)
   * stmt_1.setString(3, usuario.apellido_materno);
   * else
   * stmt_1.setNull(3, Types.VARCHAR);
   * 
   * stmt_1.setTimestamp(4, usuario.fecha_nacimiento);
   * 
   * if (usuario.telefono != null)
   * stmt_1.setLong(5, usuario.telefono);
   * else
   * stmt_1.setNull(5, Types.BIGINT);
   * 
   * stmt_1.setString(6, usuario.genero);
   * stmt_1.setString(7, usuario.email);
   * stmt_1.executeUpdate();
   * } finally {
   * stmt_1.close();
   * }
   * 
   * PreparedStatement stmt_2 = conexion.prepareStatement(
   * "DELETE FROM fotos_usuarios WHERE id_usuario=(SELECT id_usuario FROM usuarios WHERE email=?)"
   * );
   * try {
   * stmt_2.setString(1, usuario.email);
   * stmt_2.executeUpdate();
   * } finally {
   * stmt_2.close();
   * }
   * 
   * if (usuario.foto != null) {
   * PreparedStatement stmt_3 = conexion.prepareStatement(
   * "INSERT INTO fotos_usuarios(id_foto,foto,id_usuario) VALUES (0,?,(SELECT id_usuario FROM usuarios WHERE email=?))"
   * );
   * try {
   * stmt_3.setBytes(1, usuario.foto);
   * stmt_3.setString(2, usuario.email);
   * stmt_3.executeUpdate();
   * } finally {
   * stmt_3.close();
   * }
   * }
   * conexion.commit();
   * } catch (Exception e) {
   * conexion.rollback();
   * return Response.status(400).entity(j.toJson(new
   * Error(e.getMessage()))).build();
   * } finally {
   * conexion.setAutoCommit(true);
   * conexion.close();
   * }
   * return Response.ok().build();
   * }
   * 
   * @POST
   * 
   * @Path("borra_usuario")
   * 
   * @Consumes(MediaType.APPLICATION_JSON)
   * 
   * @Produces(MediaType.APPLICATION_JSON)
   * public Response borra(String json) throws Exception {
   * ParamBorraArticuloCarrito p = (ParamBorraArticuloCarrito) j.fromJson(json,
   * ParamBorraArticuloCarrito.class);
   * String email = p.email;
   * 
   * Connection conexion = pool.getConnection();
   * 
   * try {
   * PreparedStatement stmt_1 =
   * conexion.prepareStatement("SELECT 1 FROM usuarios WHERE email=?");
   * try {
   * stmt_1.setString(1, email);
   * 
   * ResultSet rs = stmt_1.executeQuery();
   * try {
   * if (!rs.next())
   * return Response.status(400).entity(j.toJson(new
   * Error("El email no existe"))).build();
   * } finally {
   * rs.close();
   * }
   * } finally {
   * stmt_1.close();
   * }
   * conexion.setAutoCommit(false);
   * PreparedStatement stmt_2 = conexion.prepareStatement(
   * "DELETE FROM fotos_usuarios WHERE id_usuario=(SELECT id_usuario FROM usuarios WHERE email=?)"
   * );
   * try {
   * stmt_2.setString(1, email);
   * stmt_2.executeUpdate();
   * } finally {
   * stmt_2.close();
   * }
   * 
   * PreparedStatement stmt_3 =
   * conexion.prepareStatement("DELETE FROM usuarios WHERE email=?");
   * try {
   * stmt_3.setString(1, email);
   * stmt_3.executeUpdate();
   * } finally {
   * stmt_3.close();
   * }
   * conexion.commit();
   * } catch (Exception e) {
   * conexion.rollback();
   * return Response.status(400).entity(j.toJson(new
   * Error(e.getMessage()))).build();
   * } finally {
   * conexion.setAutoCommit(true);
   * conexion.close();
   * }
   * return Response.ok().build();
   * }
   */
}
