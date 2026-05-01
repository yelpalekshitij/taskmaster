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
  query GetTasks($filter: TaskFilterInput, $page: Int, $size: Int) {
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
        assignee {
          id
          username
          email
          firstName
          lastName
        }
        createdBy {
          id
          username
          email
        }
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
  query GetMyTasks($status: TaskStatus, $page: Int, $size: Int) {
    myTasks(status: $status, page: $page, size: $size) {
      content {
        id
        title
        description
        status
        priority
        dueDate
        createdAt
        updatedAt
        assignee {
          id
          username
          email
        }
        createdBy {
          id
          username
          email
        }
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
      assignee {
        id
        username
        email
        firstName
        lastName
      }
      createdBy {
        id
        username
        email
      }
      tags
      comments {
        id
        content
        createdAt
        author {
          id
          username
          email
        }
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
      assignee {
        id
        username
        email
      }
      createdBy {
        id
        username
        email
      }
    }
  }
`;

const UPDATE_TASK_STATUS = gql`
  mutation UpdateTaskStatus($input: UpdateTaskStatusInput!) {
    updateTaskStatus(input: $input) {
      id
      status
      updatedAt
    }
  }
`;

const ASSIGN_TASK = gql`
  mutation AssignTask($input: AssignTaskInput!) {
    assignTask(input: $input) {
      id
      assignee {
        id
        username
        email
      }
    }
  }
`;

const ADD_COMMENT = gql`
  mutation AddComment($input: AddCommentInput!) {
    addComment(input: $input) {
      id
      content
      createdAt
      author {
        id
        username
        email
      }
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
        filter: filter ? { status: filter.status, priority: filter.priority, assigneeId: filter.assigneeId } : null,
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
      variables: { status, page, size }
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
      variables: { input }
    }).pipe(
      map(result => result.data!.updateTaskStatus)
    );
  }

  assignTask(input: AssignTaskInput): Observable<Partial<Task>> {
    return this.apollo.mutate<{ assignTask: Partial<Task> }>({
      mutation: ASSIGN_TASK,
      variables: { input }
    }).pipe(
      map(result => result.data!.assignTask)
    );
  }

  addComment(input: AddCommentInput): Observable<any> {
    return this.apollo.mutate({
      mutation: ADD_COMMENT,
      variables: { input }
    }).pipe(
      map(result => (result.data as any).addComment)
    );
  }
}
