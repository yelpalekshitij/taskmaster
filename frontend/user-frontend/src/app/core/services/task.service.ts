import { Injectable, inject } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  Task,
  TaskPage,
  CreateTaskInput,
  UpdateTaskStatusInput,
  AssignTaskInput,
  AddCommentInput,
  TaskFilter
} from '../models/task.model';

const GET_TASKS = gql`
  query GetTasks($filter: TaskFilter, $page: Int, $size: Int) {
    tasks(filter: $filter, page: $page, size: $size) {
      content {
        id
        title
        description
        status
        priority
        dueDate
        createdAt
        updatedAt
        assignedTo
        createdBy
        tags
      }
      totalElements
      totalPages
      pageNumber
      pageSize
    }
  }
`;

const GET_MY_TASKS = gql`
  query GetMyTasks($page: Int, $size: Int) {
    myTasks(page: $page, size: $size) {
      content {
        id
        title
        description
        status
        priority
        dueDate
        createdAt
        updatedAt
        assignedTo
        createdBy
        tags
      }
      totalElements
      totalPages
      pageNumber
      pageSize
    }
  }
`;

const GET_TASK = gql`
  query GetTask($id: ID!) {
    task(id: $id) {
      id
      title
      description
      status
      priority
      dueDate
      createdAt
      updatedAt
      assignedTo
      createdBy
      tags
      comments {
        id
        content
        createdAt
        userId
      }
    }
  }
`;

const CREATE_TASK = gql`
  mutation CreateTask($input: CreateTaskInput!) {
    createTask(input: $input) {
      id
      title
      description
      status
      priority
      dueDate
      createdAt
      assignedTo
      createdBy
    }
  }
`;

const UPDATE_TASK_STATUS = gql`
  mutation UpdateTaskStatus($id: ID!, $status: TaskStatus!) {
    updateTaskStatus(id: $id, status: $status) {
      id
      status
      updatedAt
    }
  }
`;

const ASSIGN_TASK = gql`
  mutation AssignTask($id: ID!, $userId: ID!) {
    assignTask(id: $id, userId: $userId) {
      id
      assignedTo
    }
  }
`;

const ADD_COMMENT = gql`
  mutation AddComment($taskId: ID!, $content: String!) {
    addComment(taskId: $taskId, content: $content) {
      id
      content
      createdAt
      userId
    }
  }
`;

@Injectable({ providedIn: 'root' })
export class TaskService {
  private apollo = inject(Apollo);

  getTasks(filter?: TaskFilter): Observable<TaskPage> {
    return this.apollo.watchQuery<{ tasks: TaskPage }>({
      query: GET_TASKS,
      variables: {
        filter: filter ? { status: filter.status, priority: filter.priority, assignedTo: filter.assigneeId } : null,
        page: filter?.page ?? 0,
        size: filter?.size ?? 20,
      }
    }).valueChanges.pipe(
      map(result => result.data.tasks)
    );
  }

  getMyTasks(status?: string, page = 0, size = 20): Observable<TaskPage> {
    return this.apollo.watchQuery<{ myTasks: TaskPage }>({
      query: GET_MY_TASKS,
      variables: { page, size }
    }).valueChanges.pipe(
      map(result => result.data.myTasks)
    );
  }

  getTask(id: string): Observable<Task> {
    return this.apollo.watchQuery<{ task: Task }>({
      query: GET_TASK,
      variables: { id }
    }).valueChanges.pipe(
      map(result => result.data.task)
    );
  }

  createTask(input: CreateTaskInput): Observable<Task> {
    return this.apollo.mutate<{ createTask: Task }>({
      mutation: CREATE_TASK,
      variables: { input },
      refetchQueries: [{ query: GET_TASKS }, { query: GET_MY_TASKS }]
    }).pipe(
      map(result => result.data!.createTask)
    );
  }

  updateTaskStatus(input: UpdateTaskStatusInput): Observable<Partial<Task>> {
    return this.apollo.mutate<{ updateTaskStatus: Partial<Task> }>({
      mutation: UPDATE_TASK_STATUS,
      variables: { id: input.taskId, status: input.status }
    }).pipe(
      map(result => result.data!.updateTaskStatus)
    );
  }

  assignTask(input: AssignTaskInput): Observable<Partial<Task>> {
    return this.apollo.mutate<{ assignTask: Partial<Task> }>({
      mutation: ASSIGN_TASK,
      variables: { id: input.taskId, userId: input.assigneeId }
    }).pipe(
      map(result => result.data!.assignTask)
    );
  }

  addComment(input: AddCommentInput): Observable<any> {
    return this.apollo.mutate({
      mutation: ADD_COMMENT,
      variables: { taskId: input.taskId, content: input.content }
    }).pipe(
      map(result => (result.data as any).addComment)
    );
  }
}
