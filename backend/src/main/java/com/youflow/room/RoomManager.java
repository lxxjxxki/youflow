package com.youflow.room;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 전체 룸 상태 관리자.
 *
 * 유저 이메일을 키로 RoomState를 메모리에서 관리한다.
 * ConcurrentHashMap으로 멀티스레드 환경에서의 맵 접근을 안전하게 처리한다.
 *
 * 개별 룸 상태 변경은 synchronized(state)로 추가 직렬화한다.
 * 이유: 같은 룸에 여러 기기가 동시에 명령을 보낼 수 있기 때문이다.
 * (예: 스마트폰과 PC에서 동시에 SEEK 명령)
 */
@Component
public class RoomManager {

    /**
     * email → RoomState 맵.
     * ConcurrentHashMap: 맵 자체의 put/get은 스레드 안전.
     * 개별 RoomState 객체 내부 변경은 별도 synchronized 필요.
     */
    private final ConcurrentHashMap<String, RoomState> rooms = new ConcurrentHashMap<>();

    /**
     * 이메일로 룸을 조회한다. 없으면 새 룸을 생성한다.
     *
     * computeIfAbsent: 키가 없을 때만 새 RoomState를 생성 → 중복 생성 방지.
     *
     * @param email 유저 이메일 (룸 ID)
     * @return 해당 유저의 RoomState
     */
    public RoomState getOrCreate(String email) {
        return rooms.computeIfAbsent(email, k -> new RoomState());
    }

    /**
     * PLAY 명령 처리.
     *
     * @param email 룸 ID
     */
    public RoomState play(String email) {
        RoomState state = getOrCreate(email);
        synchronized (state) {
            state.play();
        }
        return state;
    }

    /**
     * PAUSE 명령 처리.
     *
     * @param email 룸 ID
     */
    public RoomState pause(String email) {
        RoomState state = getOrCreate(email);
        synchronized (state) {
            state.pause();
        }
        return state;
    }

    /**
     * SEEK 명령 처리.
     *
     * @param email       룸 ID
     * @param positionSec 이동할 재생 위치 (초)
     */
    public RoomState seek(String email, double positionSec) {
        RoomState state = getOrCreate(email);
        synchronized (state) {
            state.seek(positionSec);
        }
        return state;
    }

    /**
     * CHANGE_VIDEO 명령 처리.
     *
     * @param email     룸 ID
     * @param videoId   DB Video PK
     * @param youtubeId YouTube 영상 ID
     */
    public RoomState changeVideo(String email, Long videoId, String youtubeId) {
        RoomState state = getOrCreate(email);
        synchronized (state) {
            state.changeVideo(videoId, youtubeId);
        }
        return state;
    }

    /**
     * VOLUME 명령 처리.
     *
     * @param email  룸 ID
     * @param volume 새 음량 (0~100)
     */
    public RoomState updateVolume(String email, int volume) {
        RoomState state = getOrCreate(email);
        synchronized (state) {
            state.updateVolume(volume);
        }
        return state;
    }
}
