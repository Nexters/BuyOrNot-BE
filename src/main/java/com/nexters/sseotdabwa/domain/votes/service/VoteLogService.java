package com.nexters.sseotdabwa.domain.votes.service;

import com.nexters.sseotdabwa.domain.votes.repository.VoteLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteLogService {

    private final VoteLogRepository voteLogRepository;
}
