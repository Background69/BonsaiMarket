export function resetAnswerDraft(drafts, question) {
  drafts[question.id] = {body:question.answerBody || '',baseBody:question.answerBody || '',revision:question.revision,conflict:false};
}

export function reconcileAnswerDrafts(drafts, questions) {
  for (const question of questions) {
    const draft = drafts[question.id];
    if (!draft || draft.body === draft.baseBody) resetAnswerDraft(drafts, question);
    else draft.conflict = draft.revision !== question.revision;
  }
}
