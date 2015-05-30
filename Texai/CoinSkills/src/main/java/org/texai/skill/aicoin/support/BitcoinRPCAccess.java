package org.texai.skill.aicoin.support;

import com.google.bitcoin.core.NetworkParameters;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.HttpEntity;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * BitcoinRPCAccess.java
 *
 * Description:
 *
 * Copyright (C) May 29, 2015, Stephen L. Reed.
 */
public class BitcoinRPCAccess {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(BitcoinRPCAccess.class);
  private static final String COMMAND_GET_BALANCE = "getbalance";
  private static final String COMMAND_GET_INFO = "getinfo";
  private static final String COMMAND_GET_NEW_ADDRESS = "getnewaddress";

  // the network parameters
  final NetworkParameters networkParameters;

  /**
   * Creates a new instance of BitcoinRPCAccess.
   *
   * @param networkParameters the network parameters
   */
  public BitcoinRPCAccess(final NetworkParameters networkParameters) {
    //Preconditions
    assert networkParameters != null : "networkParameters must not be null";

    this.networkParameters = networkParameters;
  }

  /**
   * Returns the bitcoind (aicoind) number of blocks.
   *
   * @return the bitcoind (aicoind) wallet balance
   */
  public int getBlocks() {
    JSONObject json = invokeRPC(
            UUID.randomUUID().toString(), // id
            COMMAND_GET_INFO, // method
            null); // params
    final JSONObject result = (JSONObject) json.get("result");
    final long blocks = (long) result.get("blocks");
    return (int) blocks;
  }

  /**
   * Returns the bitcoind (aicoind) wallet account balance.
   *
   * @param account the account
   *
   * @return the bitcoind (aicoind) wallet balance
   */
  public Double getBalance(final String account) {
    //Preconditions
    assert StringUtils.isNonEmptyString(account) : "account must be a non-empty string";

    final String[] params = {account};
    final JSONObject jsonObject = invokeRPC(
            UUID.randomUUID().toString(), // id
            COMMAND_GET_BALANCE, // method
            Arrays.asList(params)); // params
    return (Double) jsonObject.get("result");
  }

  /**
   * Returns a new address from the bitcoind (aicoind) wallet account.
   *
   * @param account the account
   *
   * @return a new address
   */
  public String getNewAddress(final String account) {
    //Preconditions
    assert StringUtils.isNonEmptyString(account) : "account must be a non-empty string";

    final String[] params = {account};
    final JSONObject jsonObject = invokeRPC(
            UUID.randomUUID().toString(), // id
            COMMAND_GET_NEW_ADDRESS, // method
            Arrays.asList(params)); // params
    return (String) jsonObject.get("result");
  }

  /**
   * Returns the bitcoind (aicoind) information.
   *
   * @return the bitcoind (aicoind) information
   */
  public JSONObject getInfo() {
    final JSONObject jsonObject = invokeRPC(
            UUID.randomUUID().toString(), // id
            COMMAND_GET_INFO, // method
            null); // params
    return (JSONObject) jsonObject.get("result");
  }

  /**
   * Performs the remote procedure call to the bitcoind (aicoind) instance.
   *
   * @param id the unique id string
   * @param method the rpc method
   * @param params the method parameters
   *
   * @return the result of the method call
   */
  @SuppressWarnings("unchecked")
  private JSONObject invokeRPC(
          final String id,
          final String method,
          final List<String> params) {
    //Preconditions
    assert StringUtils.isNonEmptyString(id) : "id must be a non-empty string";
    assert StringUtils.isNonEmptyString(method) : "method must be a non-empty string";

    final DefaultHttpClient httpclient = new DefaultHttpClient();

    final JSONObject jsonObject = new JSONObject();
    jsonObject.put("id", id);
    jsonObject.put("method", method);
    if (params != null) {
      final JSONArray jsonArray = new JSONArray();
      jsonArray.addAll(params);
      jsonObject.put("params", params);
    }
    JSONObject responseJSONObject = null;
    try {
      final int rpcPort = networkParameters.getPort() - 1;
      final AuthScope authScope = new AuthScope(
              "localhost", // host
              rpcPort); // port
      final String rpcuser = System.getenv("RPC_USER");
      if (!StringUtils.isNonEmptyString(rpcuser)) {
        throw new TexaiException("the RPC_USER environment variable must be assigned a value");
      }
      final String rpcpassword = System.getenv("RPC_PASSWORD");
      if (!StringUtils.isNonEmptyString(rpcpassword)) {
        throw new TexaiException("the RPC_PASSWORD environment variable must be assigned a value");
      }

      final Credentials credentials = new UsernamePasswordCredentials(
              rpcuser, // userName
              rpcpassword); // password
      httpclient.getCredentialsProvider().setCredentials(
              authScope,
              credentials);
      final StringEntity stringEntity = new StringEntity(jsonObject.toJSONString());
      LOGGER.info(jsonObject.toString());
      final HttpPost httppost = new HttpPost("http://localhost:" + rpcPort);
      httppost.setEntity(stringEntity);

      LOGGER.info("executing request" + httppost.getRequestLine());
      final HttpResponse response = httpclient.execute(httppost);
      HttpEntity entity = response.getEntity();

      LOGGER.info("----------------------------------------");
      LOGGER.info(response.getStatusLine());
      assert entity != null;
      LOGGER.info("Response content length: " + entity.getContentLength());
      final String jsonString = EntityUtils.toString(entity);
      LOGGER.info(jsonString);
      final JSONParser jsonParser = new JSONParser();
      responseJSONObject = (JSONObject) jsonParser.parse(jsonString);
    } catch (ParseException | IOException e) {
      throw new TexaiException(e);
    } finally {
      // release resources
      httpclient.getConnectionManager().shutdown();
    }
    return responseJSONObject;
  }

}
