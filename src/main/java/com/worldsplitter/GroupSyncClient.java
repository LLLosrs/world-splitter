package com.worldsplitter;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Blocking HTTP client for the optional self-hosted group synchronization server.
 * Callers must invoke these methods from RuneLite's background executor.
 */
@Singleton
class GroupSyncClient
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final Pattern GROUP_CODE = Pattern.compile("[A-HJ-NP-Z2-9]{6}");
	private static final Pattern MEMBER_ID = Pattern.compile("[a-f0-9]{32}");

	private final OkHttpClient okHttpClient;
	private final Gson gson;

	@Inject
	GroupSyncClient(OkHttpClient okHttpClient, Gson gson)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
	}

	static class GroupState
	{
		private String code;
		private String memberId;
		private int index;
		private int totalMembers;

		String getCode()
		{
			return code;
		}

		String getMemberId()
		{
			return memberId;
		}

		int getIndex()
		{
			return index;
		}

		int getTotalMembers()
		{
			return totalMembers;
		}
	}

	GroupState createGroup(String serverUrl) throws IOException
	{
		Request request = new Request.Builder()
			.url(baseUrl(serverUrl) + "/api/groups")
			.post(RequestBody.create(JSON, "{}"))
			.build();

		return execute(request);
	}

	GroupState joinGroup(String serverUrl, String code) throws IOException
	{
		String normalizedCode = normalizeCode(code);
		Request request = new Request.Builder()
			.url(baseUrl(serverUrl) + "/api/groups/" + normalizedCode + "/join")
			.post(RequestBody.create(JSON, "{}"))
			.build();

		return execute(request);
	}

	GroupState heartbeat(String serverUrl, String code, String memberId) throws IOException
	{
		String normalizedCode = normalizeCode(code);
		String normalizedMemberId = normalizeMemberId(memberId);
		String body = gson.toJson(new MemberBody(normalizedMemberId));
		Request request = new Request.Builder()
			.url(baseUrl(serverUrl) + "/api/groups/" + normalizedCode + "/heartbeat")
			.post(RequestBody.create(JSON, body))
			.build();

		return execute(request);
	}

	void leaveGroup(String serverUrl, String code, String memberId) throws IOException
	{
		String normalizedCode = normalizeCode(code);
		String normalizedMemberId = normalizeMemberId(memberId);
		String body = gson.toJson(new MemberBody(normalizedMemberId));
		Request request = new Request.Builder()
			.url(baseUrl(serverUrl) + "/api/groups/" + normalizedCode + "/leave")
			.post(RequestBody.create(JSON, body))
			.build();

		try (Response response = okHttpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				throw new IOException("Sync server returned HTTP " + response.code());
			}
		}
	}

	private GroupState execute(Request request) throws IOException
	{
		try (Response response = okHttpClient.newCall(request).execute())
		{
			if (!response.isSuccessful() || response.body() == null)
			{
				throw new IOException("Sync server returned HTTP " + response.code());
			}

			GroupState state = gson.fromJson(response.body().string(), GroupState.class);
			validateState(state);
			return state;
		}
	}

	private static String baseUrl(String serverUrl) throws IOException
	{
		String url = serverUrl == null ? "" : serverUrl.trim();
		while (url.endsWith("/"))
		{
			url = url.substring(0, url.length() - 1);
		}

		try
		{
			URI uri = new URI(url);
			String scheme = uri.getScheme();
			String host = uri.getHost();
			if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
				|| host == null || !("https".equalsIgnoreCase(scheme)
				|| "http".equalsIgnoreCase(scheme)))
			{
				throw new IOException("Invalid sync server URL");
			}

			boolean localHost = "localhost".equalsIgnoreCase(host)
				|| "127.0.0.1".equals(host)
				|| "::1".equals(host);
			if ("http".equalsIgnoreCase(scheme) && !localHost)
			{
				throw new IOException("Public sync servers must use HTTPS");
			}
		}
		catch (URISyntaxException e)
		{
			throw new IOException("Invalid sync server URL", e);
		}

		return url;
	}

	private static String normalizeCode(String code) throws IOException
	{
		String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
		if (!GROUP_CODE.matcher(normalized).matches())
		{
			throw new IOException("Group code must contain 6 valid characters");
		}
		return normalized;
	}

	private static String normalizeMemberId(String memberId) throws IOException
	{
		String normalized = memberId == null ? "" : memberId.trim().toLowerCase(Locale.ROOT);
		if (!MEMBER_ID.matcher(normalized).matches())
		{
			throw new IOException("Invalid member id");
		}
		return normalized;
	}

	private static void validateState(GroupState state) throws IOException
	{
		if (state == null)
		{
			throw new IOException("Sync server returned an empty response");
		}
		state.code = normalizeCode(state.code);
		state.memberId = normalizeMemberId(state.memberId);
		if (state.totalMembers < 1 || state.index < 0 || state.index >= state.totalMembers)
		{
			throw new IOException("Sync server returned invalid group state");
		}
	}

	private static class MemberBody
	{
		private final String memberId;

		private MemberBody(String memberId)
		{
			this.memberId = memberId;
		}
	}
}
