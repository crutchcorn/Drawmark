export type GetPathReqBody<
  TObj extends object,
  TKey extends keyof TObj,
  TMethod extends keyof TObj[TKey],
> = Required<TObj[TKey][TMethod]> extends { requestBody: infer TRequestBody }
  ? TRequestBody extends { content: infer TContent }
    ? TContent extends { 'application/json': infer TJson }
      ? TJson
      : never
    : never
  : never;

export type GetPathReqParams<
  TObj extends object,
  TKey extends keyof TObj,
  TMethod extends keyof TObj[TKey],
> = Required<TObj[TKey][TMethod]> extends { parameters: infer TParameters }
  ? TParameters extends { path: infer TPath }
    ? TPath
    : never
  : never;

export type GetPathQueryParams<
  TObj extends object,
  TKey extends keyof TObj,
  TMethod extends keyof TObj[TKey],
> = Required<TObj[TKey][TMethod]> extends { parameters: infer TParameters }
  ? TParameters extends { query: infer TQuery }
    ? TQuery
    : never
  : never;
